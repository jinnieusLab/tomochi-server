package com.tomochi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tomochi.auth.domain.OAuthAccountRepository;
import com.tomochi.auth.domain.OAuthProvider;
import com.tomochi.auth.domain.RefreshTokenRepository;
import com.tomochi.auth.oauth.OAuth2LoginSuccessHandler;
import com.tomochi.auth.oauth.RedisOAuth2AuthorizationRequestRepository;
import com.tomochi.support.IntegrationTest;
import com.tomochi.user.domain.PetKind;
import com.tomochi.user.domain.User;
import com.tomochi.user.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 로그인 시작 → (제공자 로그인) → 성공 처리기가 1회용 코드 발급 → 앱이 코드+verifier로 토큰 받기.
 * 제공자와 실제로 통신하는 부분은 Spring Security가 맡으니, 그 뒤의 성공 처리기부터 직접 불러요.
 */
@IntegrationTest
@AutoConfigureMockMvc
class LoginFlowTest {

	private static final String VERIFIER = "dBjftJeZ4CVP-mA92Q5qwOFxy4n2PDwq9jJlyfpoPDw";
	private static final String CHALLENGE = "uVzdJH4DNvZMUVLwHRcytsh0mwMr9gBB_R7DxseoPrw";

	@Autowired MockMvc mockMvc;
	@Autowired JsonMapper jsonMapper;
	@Autowired OAuth2LoginSuccessHandler successHandler;
	@Autowired UserRepository userRepository;
	@Autowired OAuthAccountRepository oauthAccountRepository;
	@Autowired RefreshTokenRepository refreshTokenRepository;

	@Test
	void challenge_없이는_로그인을_시작하지_않는다() throws Exception {
		mockMvc.perform(get("/api/v1/auth/oauth2/google"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CHALLENGE"));

		mockMvc.perform(get("/api/v1/auth/oauth2/google").param("challenge", "plain"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CHALLENGE"));
	}

	@Test
	void 로그인을_시작하면_제공자_로그인_페이지로_보낸다() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/auth/oauth2/github")
						.param("challenge", CHALLENGE)
						.param("petKind", "HOBBANG"))
				.andExpect(status().isFound())
				.andReturn();

		String location = result.getResponse().getRedirectedUrl();
		assertThat(location).startsWith("https://github.com/login/oauth/authorize");
		Map<String, List<String>> params = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
		assertThat(params.get("client_id")).containsExactly("test-github-client");
		assertThat(params.get("redirect_uri").getFirst())
				.isEqualTo("http://localhost/api/v1/auth/oauth2/callback/github");
		assertThat(params.get("state")).isNotEmpty();
	}

	@Test
	void 처음_로그인하면_계정을_만들고_코드를_토큰으로_바꾼다() throws Exception {
		String providerUserId = UUID.randomUUID().toString();

		String code = loginAndGetCode(OAuthProvider.GITHUB, providerUserId, "HOBBANG");
		JsonNode tokens = exchange(code, VERIFIER);

		assertThat(tokens.get("accessToken").asString()).isNotBlank();
		assertThat(tokens.get("refreshToken").asString()).isNotBlank();
		assertThat(tokens.get("expiresIn").asLong()).isEqualTo(1800);

		User user = userOf(OAuthProvider.GITHUB, providerUserId);
		assertThat(user.getPetKind()).isEqualTo(PetKind.HOBBANG);
		assertThat(user.getNickname()).isNull();
		assertThat(user.getInviteCode()).hasSize(6);
		assertThat(user.getTimezone()).isEqualTo("Asia/Seoul");
	}

	@Test
	void 펫을_안_고르면_초코다() throws Exception {
		String providerUserId = UUID.randomUUID().toString();

		loginAndGetCode(OAuthProvider.GOOGLE, providerUserId, null);

		User user = userOf(OAuthProvider.GOOGLE, providerUserId);
		assertThat(user.getPetKind()).isEqualTo(PetKind.CHOCO);
	}

	@Test
	void 다시_로그인하면_같은_계정이다() throws Exception {
		String providerUserId = UUID.randomUUID().toString();
		long before = userRepository.count();

		loginAndGetCode(OAuthProvider.GITHUB, providerUserId, "BUTTER");
		loginAndGetCode(OAuthProvider.GITHUB, providerUserId, "PEANUT");

		assertThat(userRepository.count()).isEqualTo(before + 1);
		User user = userOf(OAuthProvider.GITHUB, providerUserId);
		assertThat(user.getPetKind()).isEqualTo(PetKind.BUTTER);
	}

	@Test
	void 같은_id라도_제공자가_다르면_다른_계정이다() throws Exception {
		String providerUserId = UUID.randomUUID().toString();

		loginAndGetCode(OAuthProvider.GITHUB, providerUserId, null);
		loginAndGetCode(OAuthProvider.GOOGLE, providerUserId, null);

		Long githubUser = userOf(OAuthProvider.GITHUB, providerUserId).getId();
		Long googleUser = userOf(OAuthProvider.GOOGLE, providerUserId).getId();
		assertThat(githubUser).isNotEqualTo(googleUser);
	}

	@Test
	void 코드는_한_번만_쓸_수_있다() throws Exception {
		String code = loginAndGetCode(OAuthProvider.GITHUB, UUID.randomUUID().toString(), null);
		exchange(code, VERIFIER);

		mockMvc.perform(tokenRequest(code, VERIFIER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_CODE_INVALID"));
	}

	@Test
	void verifier가_틀리면_토큰을_주지_않고_코드도_버린다() throws Exception {
		String code = loginAndGetCode(OAuthProvider.GITHUB, UUID.randomUUID().toString(), null);
		String wrongVerifier = "x".repeat(43);

		mockMvc.perform(tokenRequest(code, wrongVerifier))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_CODE_INVALID"));

		// 코드가 새도 verifier를 여러 번 맞혀 볼 수 없어요.
		mockMvc.perform(tokenRequest(code, VERIFIER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_CODE_INVALID"));
	}

	@Test
	void 없는_코드는_거절한다() throws Exception {
		mockMvc.perform(tokenRequest("no-such-code", VERIFIER))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("AUTH_CODE_INVALID"));
	}

	@Test
	void Refresh_Token은_원문_대신_해시로_저장한다() throws Exception {
		String code = loginAndGetCode(OAuthProvider.GITHUB, UUID.randomUUID().toString(), null);
		String refreshToken = exchange(code, VERIFIER).get("refreshToken").asString();

		assertThat(refreshTokenRepository.findByTokenHash(refreshToken)).isEmpty();
		assertThat(refreshTokenRepository.findByTokenHash(sha256Hex(refreshToken)))
				.hasValueSatisfying(token -> {
					assertThat(token.getDeviceName()).isEqualTo("내 맥북");
					assertThat(token.getRevokedAt()).isNull();
				});
	}

	/** 연관 User는 지연 로딩이라, id만 꺼내 다시 조회해요. */
	private User userOf(OAuthProvider provider, String providerUserId) {
		Long userId = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
				.orElseThrow().getUser().getId();
		return userRepository.findById(userId).orElseThrow();
	}

	/** Spring Security가 제공자 로그인을 마친 직후의 상태를 만들어 성공 처리기를 불러요. */
	private String loginAndGetCode(OAuthProvider provider, String providerUserId, String petKind) throws Exception {
		Map<String, Object> attributes = new java.util.HashMap<>();
		attributes.put("challenge", CHALLENGE);
		if (petKind != null) {
			attributes.put("petKind", petKind);
		}
		OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
				.authorizationUri("https://provider.example/authorize")
				.clientId("client")
				.state("state")
				.attributes(attributes)
				.build();

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RedisOAuth2AuthorizationRequestRepository.REMOVED_REQUEST_ATTRIBUTE, authorizationRequest);
		MockHttpServletResponse response = new MockHttpServletResponse();
		DefaultOAuth2User oauth2User = new DefaultOAuth2User(
				List.of(new SimpleGrantedAuthority("OAUTH2_USER")), Map.of("id", providerUserId), "id");
		OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
				oauth2User, oauth2User.getAuthorities(), provider.name().toLowerCase());

		successHandler.onAuthenticationSuccess(request, response, authentication);

		String redirect = response.getRedirectedUrl();
		assertThat(redirect).startsWith("tomochi://auth/callback?code=");
		return UriComponentsBuilder.fromUriString(redirect).build().getQueryParams().getFirst("code");
	}

	private JsonNode exchange(String code, String verifier) throws Exception {
		MvcResult result = mockMvc.perform(tokenRequest(code, verifier))
				.andExpect(status().isOk())
				.andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString());
	}

	private org.springframework.test.web.servlet.RequestBuilder tokenRequest(String code, String verifier) {
		return post("/api/v1/auth/token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(Map.of(
						"code", code, "codeVerifier", verifier, "deviceName", "내 맥북")));
	}

	private static String sha256Hex(String value) throws Exception {
		return HexFormat.of().formatHex(
				MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)));
	}
}
