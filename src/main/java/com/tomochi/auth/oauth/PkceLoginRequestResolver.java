package com.tomochi.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * 기본 로그인 요청에 앱의 {@code challenge}·{@code petKind}를 더해요.
 * challenge 형식은 앞에서 {@link LoginStartValidationFilter}가 확인해요.
 */
public class PkceLoginRequestResolver implements OAuth2AuthorizationRequestResolver {

	private final DefaultOAuth2AuthorizationRequestResolver delegate;

	public PkceLoginRequestResolver(ClientRegistrationRepository repository) {
		this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
				repository, LoginRequestAttributes.AUTHORIZATION_BASE_URI);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
		return withAppAttributes(delegate.resolve(request), request);
	}

	@Override
	public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
		return withAppAttributes(delegate.resolve(request, clientRegistrationId), request);
	}

	private OAuth2AuthorizationRequest withAppAttributes(OAuth2AuthorizationRequest authorizationRequest,
			HttpServletRequest request) {
		if (authorizationRequest == null) {
			return null;
		}
		String challenge = request.getParameter(LoginRequestAttributes.CHALLENGE);
		String petKind = request.getParameter(LoginRequestAttributes.PET_KIND);
		return OAuth2AuthorizationRequest.from(authorizationRequest)
				.attributes(attributes -> {
					attributes.put(LoginRequestAttributes.CHALLENGE, challenge);
					if (petKind != null) {
						attributes.put(LoginRequestAttributes.PET_KIND, petKind);
					}
				})
				.build();
	}
}
