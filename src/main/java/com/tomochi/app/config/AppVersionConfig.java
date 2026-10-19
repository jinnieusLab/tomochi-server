package com.tomochi.app.config;

import com.tomochi.global.error.ErrorResponseWriter;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppVersionConfig {

	/** Spring Security 필터보다 앞에 둬요. */
	@Bean
	public FilterRegistrationBean<MinimumAppVersionFilter> minimumAppVersionFilter(
			AppVersionProperties properties, ErrorResponseWriter errorResponseWriter) {
		FilterRegistrationBean<MinimumAppVersionFilter> registration =
				new FilterRegistrationBean<>(new MinimumAppVersionFilter(properties, errorResponseWriter));
		registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 10);
		return registration;
	}
}
