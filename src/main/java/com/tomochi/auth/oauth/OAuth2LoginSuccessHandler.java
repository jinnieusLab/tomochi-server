package com.tomochi.auth.oauth;

import com.tomochi.auth.config.AuthProperties;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.service.AuthCodeService;
import com.tomochi.auth.service.OAuthLoginFacade;
import com.tomochi.user.domain.PetKind;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 제공자 로그인이 끝나면 사용자를 찾거나 만들고, 1회용 코드를 실어 맥 앱으로 돌려보내요.
 * 토큰은 여기서 주지 않아요. 앱이 코드와 verifier로 {@code POST /api/v1/auth/token}을 불러야 받아요.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final OAuthLoginFacade oauthLoginFacade;
	private final AuthCodeService authCodeService;
	private final AuthProperties authProperties;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
		OAuth2AuthorizationRequest authorizationRequest = (OAuth2AuthorizationRequest)
				request.getAttribute(RedisOAuth2AuthorizationRequestRepository.REMOVED_REQUEST_ATTRIBUTE);

		OAuthProvider provider = OAuthProvider.fromRegistrationId(token.getAuthorizedClientRegistrationId())
				.orElseThrow();
		// Google은 sub, GitHub는 id. 이메일처럼 바뀔 수 있는 값이 아니라 제공자 안에서 고정된 id예요.
		String providerUserId = token.getName();
		PetKind petKind = PetKind.fromOrDefault(LoginRequestAttributes.petKind(authorizationRequest));

		Long userId = oauthLoginFacade.login(provider, providerUserId, petKind);
		String code = authCodeService.issue(userId, LoginRequestAttributes.challenge(authorizationRequest));

		response.sendRedirect(UriComponentsBuilder.fromUriString(authProperties.appRedirectUri())
				.queryParam("code", code)
				.build()
				.toUriString());
	}
}
