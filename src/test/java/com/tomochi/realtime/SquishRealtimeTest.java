package com.tomochi.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.auth.service.TokenService;
import com.tomochi.realtime.RealtimeConnectionTest.Client;
import com.tomochi.squish.service.SquishService;
import com.tomochi.support.TestcontainersConfiguration;
import com.tomochi.user.domain.PetKind;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMetrics
class SquishRealtimeTest {

	@Value("${local.server.port}")
	int port;

	@Autowired SquishService squishService;
	@Autowired OAuthLoginFacade oauthLoginFacade;
	@Autowired TokenService tokenService;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired JsonMapper jsonMapper;
	@Autowired MeterRegistry meterRegistry;

	@Test
	void 연결된_친구에게_꾹을_바로_보내고_ack하면_전달_완료다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		Client client = connect(minji);

		squishService.send(me, minji);

		JsonNode received = client.next();
		assertThat(received.get("type").asString()).isEqualTo("squish.received");
		assertThat(received.at("/data/fromFriendId").asLong()).isEqualTo(me);
		long squishId = received.at("/data/squishId").asLong();
		assertThat(deliveredAt(squishId)).isNull();

		client.send("{\"type\": \"squish.ack\", \"data\": {\"squishIds\": [" + squishId + "]}}");
		client.send("{\"type\": \"ping\", \"data\": {}}");
		assertThat(client.next().get("type").asString()).isEqualTo("pong");

		assertThat(deliveredAt(squishId)).isNotNull();
		assertThat(meterRegistry.find("tomochi.squish.delivery").timer().count()).isPositive();
		client.close();
	}

	@Test
	void 남의_꾹은_ack할_수_없다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		Long other = newUser();
		befriend(me, minji);
		befriend(me, other);
		Client minjiClient = connect(minji);
		squishService.send(me, minji);
		long squishId = minjiClient.next().at("/data/squishId").asLong();
		Client otherClient = connect(other);

		otherClient.send("{\"type\": \"squish.ack\", \"data\": {\"squishIds\": [" + squishId + "]}}");
		otherClient.send("{\"type\": \"ping\", \"data\": {}}");
		otherClient.next();

		assertThat(deliveredAt(squishId)).isNull();
		minjiClient.close();
		otherClient.close();
	}

	@Test
	void 연결하면_꺼져_있던_동안의_꾹을_묶어서_한_번_보낸다() throws Exception {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);
		squishService.send(me, minji);

		Client client = connect(minji);

		JsonNode pending = client.next();
		assertThat(pending.get("type").asString()).isEqualTo("squish.pending");
		assertThat(pending.at("/data/items/0/fromFriendId").asLong()).isEqualTo(me);
		assertThat(pending.at("/data/items/0/count").asInt()).isEqualTo(1);
		client.close();
	}

	@Test
	void 연결이_없으면_전달_대기로_남는다() {
		Long me = newUser();
		Long minji = newUser();
		befriend(me, minji);

		squishService.send(me, minji);

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM squish WHERE to_user_id = ? AND delivered_at IS NULL", Integer.class, minji))
				.isEqualTo(1);
	}

	private Object deliveredAt(long squishId) {
		return jdbcTemplate.queryForObject("SELECT delivered_at FROM squish WHERE id = ?", Object.class, squishId);
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
