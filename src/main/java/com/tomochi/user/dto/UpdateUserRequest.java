package com.tomochi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 내 정보 일부 바꾸기 (PATCH). 필드마다 "안 보냄"과 "null로 보냄"을 구분해야 해서 레코드 대신 세터를 써요.
 * Jackson은 JSON에 키가 있을 때만 세터를 불러서, 세터가 불렸는지로 보냈는지를 알 수 있어요.
 * (레코드의 {@code Optional} 필드는 Jackson 3에서 키가 없어도 {@code Optional.empty()}가 돼서 둘을 구분하지 못해요.)
 *
 * <ul>
 *   <li>키 없음 → 그대로 둠</li>
 *   <li>{@code "nickname": null} 또는 빈 문자열 → 지움</li>
 *   <li>{@code "nickname": "민지"} → 바꿈</li>
 * </ul>
 */
public class UpdateUserRequest {

	@Schema(description = "앞뒤 공백을 지우고 1~12자. 빈 문자열이나 null이면 지워요", example = "민지", nullable = true)
	private String nickname;
	private boolean nicknameSent;

	@Schema(description = "펫 종류. 6종 중 하나이고 지울 수 없어요", example = "HOBBANG",
			allowableValues = {"CHOCO", "BUTTER", "HOBBANG", "SHERBET", "PEANUT", "HONEYCOMB"})
	private String petKind;
	private boolean petKindSent;

	public void setNickname(String nickname) {
		this.nickname = nickname;
		this.nicknameSent = true;
	}

	public boolean hasNickname() {
		return nicknameSent;
	}

	public String nickname() {
		return nickname;
	}

	public void setPetKind(String petKind) {
		this.petKind = petKind;
		this.petKindSent = true;
	}

	public boolean hasPetKind() {
		return petKindSent;
	}

	public String petKind() {
		return petKind;
	}
}
