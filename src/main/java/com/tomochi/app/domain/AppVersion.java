package com.tomochi.app.domain;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 맥 앱 버전 {@code major.minor.patch}. 문자열로 비교하면 "0.10.0" < "0.9.0"이 돼서 숫자로 나눠 비교해요.
 */
public record AppVersion(int major, int minor, int patch) implements Comparable<AppVersion> {

	private static final Pattern FORMAT = Pattern.compile("^(\\d{1,4})\\.(\\d{1,4})\\.(\\d{1,4})$");

	public static AppVersion parse(String value) {
		return tryParse(value).orElseThrow(() -> new IllegalArgumentException("버전 형식이 아니에요: " + value));
	}

	public static Optional<AppVersion> tryParse(String value) {
		if (value == null) {
			return Optional.empty();
		}
		Matcher matcher = FORMAT.matcher(value.strip());
		if (!matcher.matches()) {
			return Optional.empty();
		}
		return Optional.of(new AppVersion(
				Integer.parseInt(matcher.group(1)),
				Integer.parseInt(matcher.group(2)),
				Integer.parseInt(matcher.group(3))));
	}

	public boolean isOlderThan(AppVersion other) {
		return compareTo(other) < 0;
	}

	@Override
	public int compareTo(AppVersion other) {
		if (major != other.major) {
			return Integer.compare(major, other.major);
		}
		if (minor != other.minor) {
			return Integer.compare(minor, other.minor);
		}
		return Integer.compare(patch, other.patch);
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch;
	}
}
