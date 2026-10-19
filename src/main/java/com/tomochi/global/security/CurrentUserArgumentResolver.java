package com.tomochi.global.security;

import com.tomochi.auth.error.AuthErrorCode;
import com.tomochi.auth.service.TokenService;
import com.tomochi.global.error.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(CurrentUser.class)
				&& parameter.getParameterType().equals(AuthUser.class);
	}

	@Override
	public AuthUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(AuthErrorCode.UNAUTHENTICATED);
		}
		String sessionId = jwt.getClaimAsString(TokenService.SESSION_ID_CLAIM);
		if (sessionId == null) {
			throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
		}
		return new AuthUser(Long.valueOf(jwt.getSubject()), sessionId);
	}
}
