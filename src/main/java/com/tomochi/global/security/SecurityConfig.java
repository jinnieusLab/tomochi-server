package com.tomochi.global.security;

import com.tomochi.auth.oauth.LoginRequestAttributes;
import com.tomochi.auth.oauth.LoginStartValidationFilter;
import com.tomochi.auth.oauth.OAuth2LoginFailureHandler;
import com.tomochi.auth.oauth.OAuth2LoginSuccessHandler;
import com.tomochi.auth.oauth.PkceLoginRequestResolver;
import com.tomochi.auth.oauth.RedisOAuth2AuthorizationRequestRepository;
import com.tomochi.global.error.ErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 맥 앱 전용 API라 세션·쿠키·CSRF·CORS를 쓰지 않아요.
 * 로그인은 OAuth2 Client(구글·깃허브), 그 뒤 요청은 Resource Server(JWT Bearer)가 맡아요.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			LoginRequestAttributes.AUTHORIZATION_BASE_URI + "/**",
			"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
			"/error",
	};

	private final RedisOAuth2AuthorizationRequestRepository authorizationRequestRepository;
	private final OAuth2LoginSuccessHandler loginSuccessHandler;
	private final OAuth2LoginFailureHandler loginFailureHandler;
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final JsonAccessDeniedHandler accessDeniedHandler;
	private final ErrorResponseWriter errorResponseWriter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			ClientRegistrationRepository clientRegistrationRepository) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.requestCache(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
						.anyRequest().authenticated())
				.oauth2Login(login -> login
						.authorizationEndpoint(endpoint -> endpoint
								.baseUri(LoginRequestAttributes.AUTHORIZATION_BASE_URI)
								.authorizationRequestResolver(new PkceLoginRequestResolver(clientRegistrationRepository))
								.authorizationRequestRepository(authorizationRequestRepository))
						.redirectionEndpoint(endpoint -> endpoint.baseUri(LoginRequestAttributes.CALLBACK_URI))
						.successHandler(loginSuccessHandler)
						.failureHandler(loginFailureHandler))
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.addFilterBefore(new LoginStartValidationFilter(errorResponseWriter),
						OAuth2AuthorizationRequestRedirectFilter.class);
		return http.build();
	}
}
