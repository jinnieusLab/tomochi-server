package com.tomochi.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.global.security.AuthUser;
import com.tomochi.presence.domain.PresenceStatus;
import com.tomochi.presence.domain.StatusIcon;
import com.tomochi.presence.dto.PresenceRequest;
import com.tomochi.presence.service.PresenceService;
import com.tomochi.realtime.RealtimeConnectionTest.Client;
import com.tomochi.support.TestcontainersConfiguration;
import com.tomochi.user.domain.PetKind;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PresenceRealtimeTest {

	@Value("${local.server.port}")
	int port;

	@Autowired PresenceService presenceService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired JsonMapper jsonMapper;

	@Test
	void 친구의_상태가_바뀌면_바로_알리고_같은_하트비트로는_알리지_않는다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		Client minjiClient = connect(minji);
		AuthUser meOnMac = new AuthUser(me, "mac");

		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.WORKING, StatusIcon.CODE));

		JsonNode changed = minjiClient.next();
		assertThat(changed.get("type").asString()).isEqualTo("presence.changed");
		assertThat(changed.at("/data/friendId").asLong()).isEqualTo(me);
		assertThat(changed.at("/data/status").asString()).isEqualTo("WORKING");
		assertThat(changed.at("/data/icon").asString()).isEqualTo("CODE");

		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.WORKING, StatusIcon.CODE));
		minjiClient.send("{\"type\": \"ping\", \"data\": {}}");
		assertThat(minjiClient.next().get("type").asString()).isEqualTo("pong");

		presenceService.heartbeat(meOnMac, new PresenceRequest(PresenceStatus.RESTING, null));
		assertThat(minjiClient.next().at("/data/status").asString()).isEqualTo("RESTING");
		minjiClient.close();
	}

	@Test
	void 나를_숨긴_친구에게는_알리지_않는다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		jdbcTemplate.update("INSERT INTO friend_setting (owner_id, friend_id, hidden, updated_at) VALUES (?, ?, TRUE, UTC_TIMESTAMP(6))",
				minji, me);
		Client minjiClient = connect(minji);

		presenceService.heartbeat(new AuthUser(me, "mac"), new PresenceRequest(PresenceStatus.WORKING, null));
		minjiClient.send("{\"type\": \"ping\", \"data\": {}}");

		assertThat(minjiClient.next().get("type").asString()).isEqualTo("pong");
		minjiClient.close();
	}

	@Test
	void 실시간_연결이_끊기면_바로_오프라인으로_알린다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		Client minjiClient = connect(minji);
		Client meClient = connect(me);

		meClient.send("{\"type\": \"presence.update\", \"data\": {\"status\": \"WORKING\"}}");
		assertThat(minjiClient.next().at("/data/status").asString()).isEqualTo("WORKING");

		meClient.close();

		assertThat(minjiClient.next().at("/data/status").asString()).isEqualTo("OFFLINE");
		minjiClient.close();
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

	private Long newUser() {
		return oauthLoginFacade.login(OAuthProvider.GITHUB, UUID.randomUUID().toString(), PetKind.CHOCO);
	}

	private void befriend(Long a, Long b) {
		jdbcTemplate.update("INSERT INTO friendship (user_low, user_high, created_at) VALUES (?, ?, UTC_TIMESTAMP(6))",
				Math.min(a, b), Math.max(a, b));
	}
}
