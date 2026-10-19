package com.tomochi.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI tomochiOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Tomochi API")
						.description("토모찌 맥 앱이 쓰는 서버 API")
						.version("v1"));
	}
}
