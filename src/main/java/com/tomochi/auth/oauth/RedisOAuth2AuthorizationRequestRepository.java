package com.tomochi.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

/**
 * 구글·깃허브로 갔다 돌아오는 동안의 로그인 요청을 세션 대신 Redis에 둬요.
 * 키는 OAuth2 {@code state} 값이라, 돌아온 요청의 state로만 꺼낼 수 있어요 (CSRF 방지).
 * 서버가 무상태라 여러 대로 늘어나도 어느 서버로 돌아와도 돼요.
 */
@Component
public class RedisOAuth2AuthorizationRequestRepository
		implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

	/** 꺼낸 요청을 성공 처리기에 넘길 때 쓰는 요청 속성 이름 */
	public static final String REMOVED_REQUEST_ATTRIBUTE =
			RedisOAuth2AuthorizationRequestRepository.class.getName() + ".REMOVED";

	private static final String KEY_PREFIX = "auth:oauth2:";
	private static final Duration TTL = Duration.ofMinutes(10);

	private final RedisTemplate<String, Object> redisTemplate;

	public RedisOAuth2AuthorizationRequestRepository(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new JdkSerializationRedisSerializer());
		template.afterPropertiesSet();
		this.redisTemplate = template;
	}

	@Override
	public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
		String state = request.getParameter(OAuth2ParameterNames.STATE);
		if (state == null) {
			return null;
		}
		return (OAuth2AuthorizationRequest) redisTemplate.opsForValue().get(KEY_PREFIX + state);
	}

	@Override
	public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
			HttpServletRequest request, HttpServletResponse response) {
		if (authorizationRequest == null) {
			removeAuthorizationRequest(request, response);
			return;
		}
		redisTemplate.opsForValue().set(KEY_PREFIX + authorizationRequest.getState(), authorizationRequest, TTL);
	}

	/**
	 * 꺼내면서 지워요 (GETDEL). 같은 state로 두 번 돌아와도 한 번만 로그인돼요.
	 * 로그인이 끝난 뒤 성공 처리기가 challenge를 읽을 수 있게 요청 속성에 남겨 둬요.
	 */
	@Override
	public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
			HttpServletResponse response) {
		String state = request.getParameter(OAuth2ParameterNames.STATE);
		if (state == null) {
			return null;
		}
		OAuth2AuthorizationRequest removed =
				(OAuth2AuthorizationRequest) redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + state);
		if (removed != null) {
			request.setAttribute(REMOVED_REQUEST_ATTRIBUTE, removed);
		}
		return removed;
	}
}
