package com.tomochi.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearer";

	/** Swagger UI의 Authorize 버튼에 Access Token을 넣으면 보호된 API를 바로 불러 볼 수 있어요. */
	@Bean
	public OpenAPI tomochiOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("Tomochi API")
						.description("토모찌 맥 앱이 쓰는 서버 API")
						.version("v1"))
				.components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}
}
