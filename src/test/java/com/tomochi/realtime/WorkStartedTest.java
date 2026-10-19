package com.tomochi.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.dto.PresenceRequest;
import com.tomochi.presence.service.PresenceService;
import com.tomochi.realtime.RealtimeConnectionTest.Client;
import com.tomochi.support.TestcontainersConfiguration;
import com.tomochi.user.domain.PetKind;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class WorkStartedTest {

	@Value("${local.server.port}")
	int port;

	@Autowired PresenceService presenceService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired StringRedisTemplate redisTemplate;
	@Autowired JsonMapper jsonMapper;

	@Test
	void 작업을_시작하면_친구에게_펫과_함께_알린다() throws Exception {
		Long me = newUser(PetKind.HONEYCOMB);
		Long minji = newUser(PetKind.CHOCO);
		befriend(me, minji);
		Client minjiClient = connect(minji);

		presenceService.heartbeat(new AuthUser(me, "mac"), new PresenceRequest(PresenceStatus.WORKING, null));

		List<JsonNode> messages = drainUntilPong(minjiClient);
		JsonNode started = messages.stream().filter(m -> m.get("type").asString().equals("friend.workStarted"))
				.findFirst().orElseThrow();
		assertThat(started.at("/data/friendId").asLong()).isEqualTo(me);
		assertThat(started.at("/data/petKind").asString()).isEqualTo("HONEYCOMB");
		minjiClient.close();
	}

	@Test
	void 쉬었다가_다시_시작해도_15분_안에는_한_번만_알린다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);
		befriend(me, minji);
		Client minjiClient = connect(minji);
		AuthUser meOnMac = new AuthUser(me, "mac");

		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.WORKING, null));
		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.RESTING, null));
		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.WORKING, null));

		long workStarted = drainUntilPong(minjiClient).stream()
				.filter(m -> m.get("type").asString().equals("friend.workStarted")).count();
		assertThat(workStarted).isEqualTo(1);

		// 15분이 지나면 다시 알려요.
		redisTemplate.delete("friend:workStarted:" + me);
		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.RESTING, null));
		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.WORKING, null));
		assertThat(drainUntilPong(minjiClient).stream()
				.filter(m -> m.get("type").asString().equals("friend.workStarted")).count()).isEqualTo(1);
		minjiClient.close();
	}

	@Test
	void 나를_숨긴_친구에게는_알리지_않는다() throws Exception {
		Long me = newUser(PetKind.CHOCO);
		Long minji = newUser(PetKind.CHOCO);
		befriend(me, minji);
		jdbcTemplate.update("INSERT INTO friend_setting (owner_id, friend_id, hidden, updated_at) VALUES (?, ?, TRUE, UTC_TIMESTAMP(6))",
				minji, me);
		Client minjiClient = connect(minji);

		presenceService.heartbeat(new AuthUser(me, "mac"), new PresenceRequest(PresenceStatus.WORKING, null));

		assertThat(drainUntilPong(minjiClient)).noneMatch(m -> m.get("type").asString().equals("friend.workStarted"));
		minjiClient.close();
	}

	/** ping을 보내고 pong이 올 때까지 받은 메시지들 (pong 앞까지) */
	private static List<JsonNode> drainUntilPong(Client client) throws Exception {
		client.send("{\"type\": \"ping\", \"data\": {}}");
		List<JsonNode> messages = new ArrayList<>();
		for (JsonNode message = client.next(); !message.get("type").asString().equals("pong"); message = client.next()) {
			messages.add(message);
		}
		return messages;
	}

	private Client connect(Long userId) throws Exception {
		Client client = new Client(jsonMapper);
		WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
		headers.add("Authorization", "Bearer " + tokenService.issueForNewLogin(userId, "맥북").accessToken());
		client.session = new StandardWebSocketClient()
				.execute(client, headers, URI.create("ws://localhost:" + port + "/api/v1/ws"))
				.get(5, TimeUnit.SECONDS);
		return client;
	}

	private Long newUser(PetKind petKind) {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), petKind);
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}
}
