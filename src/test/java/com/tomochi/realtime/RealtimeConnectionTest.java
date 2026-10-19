package com.tomochi.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.service.PresenceStore;
import com.tomochi.support.TestcontainersConfiguration;
import com.tomochi.user.domain.PetKind;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 실제 포트로 붙어 봐요. 메시지는 큐에 모아 5초 안에 오는지 확인해요. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class RealtimeConnectionTest {

	@Value("${local.server.port}")
	int port;

	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired UserMessageSender userMessageSender;
	@Autowired PresenceStore presenceStore;
	@Autowired JsonMapper jsonMapper;
	@Autowired JwtEncoder jwtEncoder;

	@Test
	void Bearer_토큰으로_연결하고_ping에_pong으로_답한다() throws Exception {
		Client client = connect(accessToken(newUser()));

		client.send("{\"type\": \"ping\", \"data\": {}}");

		assertThat(client.next().get("type").asString()).isEqualTo("pong");
		client.close();
	}

	@Test
	void 토큰이_없거나_틀리면_연결되지_않는다() {
		assertThatThrownBy(() -> connect(null)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> connect("not-a-jwt")).isInstanceOf(Exception.class);
	}

	@Test
	void 서버가_그_사용자에게_메시지를_보낸다() throws Exception {
		Long userId = newUser();
		Client client = connect(accessToken(userId));

		boolean sent = userMessageSender.send(userId, "squish.received", Map.of("fromFriendId", 7));

		assertThat(sent).isTrue();
		JsonNode message = client.next();
		assertThat(message.get("type").asString()).isEqualTo("squish.received");
		assertThat(message.at("/data/fromFriendId").asInt()).isEqualTo(7);
		client.close();
	}

	@Test
	void 기기_여러_대에_모두_보내고_연결이_없으면_false() throws Exception {
		Long userId = newUser();
		Client macbook = connect(accessToken(userId));
		Client imac = connect(accessToken(userId));

		userMessageSender.send(userId, "friend.added", Map.of());

		assertThat(macbook.next().get("type").asString()).isEqualTo("friend.added");
		assertThat(imac.next().get("type").asString()).isEqualTo("friend.added");
		macbook.close();
		imac.close();
		// 서버가 연결 해제를 처리할 때까지 기다려요.
		boolean stillSent = true;
		for (int i = 0; i < 50 && stillSent; i++) {
			Thread.sleep(100);
			stillSent = userMessageSender.send(userId, "friend.added", Map.of());
		}
		assertThat(stillSent).isFalse();
	}

	@Test
	void presence_update_메시지로_하트비트를_보낸다() throws Exception {
		Long userId = newUser();
		Client client = connect(accessToken(userId));

		client.send("{\"type\": \"presence.update\", \"data\": {\"status\": \"WORKING\", \"icon\": \"CODE\"}}");
		client.send("{\"type\": \"ping\", \"data\": {}}");
		assertThat(client.next().get("type").asString()).isEqualTo("pong");

		assertThat(presenceStore.find(userId, Instant.now()).status()).isEqualTo(PresenceStatus.WORKING);
		client.close();
	}

	@Test
	void 토큰_만료_1분_전에_token_expiring을_보낸다() throws Exception {
		Instant now = Instant.now();
		String shortLived = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder().issuer("tomochi").subject(String.valueOf(newUser())).claim("sid", "s1")
						.issuedAt(now).expiresAt(now.plusSeconds(62)).build())).getTokenValue();

		Client client = connect(shortLived);

		JsonNode message = client.next();
		assertThat(message.get("type").asString()).isEqualTo("token.expiring");
		assertThat(message.at("/data/expiresAt").asString()).isNotBlank();
		client.close();
	}

	@Test
	void 모르는_메시지에는_오류로_답한다() throws Exception {
		Client client = connect(accessToken(newUser()));

		client.send("{\"type\": \"hello\"}");
		assertThat(client.next().at("/data/code").asString()).isEqualTo("UNKNOWN_TYPE");
		client.send("not json");
		assertThat(client.next().at("/data/code").asString()).isEqualTo("MALFORMED_MESSAGE");
		client.close();
	}

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}

	private String accessToken(Long userId) {
		return tokenService.issueForNewLogin(userId, "맥북").accessToken();
	}

	private Client connect(String accessToken) throws Exception {
		Client client = new Client(jsonMapper);
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		if (accessToken != null) {
			headers.add("Authorization", "Bearer " + accessToken);
		}
		client.session = new StandardWebSocketClient()
				.execute(client, headers, URI.create("ws://localhost:" + port + "/api/v1/ws"))
				.get(5, TimeUnit.SECONDS);
		client.awaitReady();
		return client;
	}

	static class Client extends TextWebSocketHandler {

		private final BlockingDeque<String> received = new LinkedBlockingDeque<>();
		private final JsonMapper jsonMapper;
		WebSocketSession session;

		Client(JsonMapper jsonMapper) {
			this.jsonMapper = jsonMapper;
		}

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			received.add(message.getPayload());
		}

		void send(String payload) throws Exception {
			session.sendMessage(new TextMessage(payload));
		}

		/**
		 * 클라이언트 쪽 연결이 끝나도 서버가 세션을 등록하기 전일 수 있어요. ping을 보내 pong을 받으면 서버가 등록을 마친 거예요.
		 * 그 사이에 온 다른 메시지(squish.pending, token.expiring 등)는 받은 순서대로 다시 넣어 둬요.
		 */
		void awaitReady() throws Exception {
			send("{\"type\": \"ping\", \"data\": {}}");
			List<String> early = new ArrayList<>();
			for (int i = 0; i < 20; i++) {
				String payload = received.poll(5, TimeUnit.SECONDS);
				assertThat(payload).as("연결 후 pong이 와야 해요").isNotNull();
				if (jsonMapper.readTree(payload).get("type").asString().equals("pong")) {
					for (int j = early.size() - 1; j >= 0; j--) {
						received.addFirst(early.get(j));
					}
					return;
				}
				early.add(payload);
			}
			throw new AssertionError("pong이 오지 않았어요");
		}

		JsonNode next() throws Exception {
			String payload = received.poll(5, TimeUnit.SECONDS);
			assertThat(payload).as("5초 안에 메시지가 와야 해요").isNotNull();
			return jsonMapper.readTree(payload);
		}

		void close() throws Exception {
			session.close(CloseStatus.NORMAL);
		}
	}
}
