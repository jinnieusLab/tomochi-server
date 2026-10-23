// 테스트 사용자의 Access Token을 k6 안에서 직접 만들어요.
//
// 서버는 Access Token을 받을 때 서명(HS256)·만료(exp)·발급자(iss)만 확인하고 DB를 보지 않아요 (JwtConfig).
// 그래서 같은 JWT_SECRET으로 서명하면 OAuth 로그인 없이도 그 사용자로 요청할 수 있어요.
// 서버에 테스트용 발급 API를 따로 두지 않아서 운영에 새는 구멍이 생기지 않아요.
// 대신 JWT_SECRET은 부하 테스트 환경 전용 값이어야 하고, 실행할 때 -e로만 넘겨요.
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const ISSUER = 'tomochi';

function base64url(value) {
	return encoding.b64encode(value, 'rawurl');
}

/**
 * @param {number} userId     시드로 넣은 사용자 id (JWT sub)
 * @param {string} secret     서버의 JWT_SECRET (Base64)
 * @param {number} ttlSeconds 테스트 시간보다 길게. 짧으면 실시간 연결이 만료(4001)로 끊겨요.
 */
export function accessToken(userId, secret, ttlSeconds = 3 * 60 * 60) {
	const now = Math.floor(Date.now() / 1000);
	const header = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
	const payload = base64url(JSON.stringify({
		iss: ISSUER,
		sub: String(userId),
		// 세션(기기) id. 서버는 로그아웃·기기 구분에만 써서 아무 값이어도 돼요.
		sid: `loadtest-${userId}`,
		iat: now,
		exp: now + ttlSeconds,
	}));
	const key = encoding.b64decode(secret, 'std');
	const signature = base64url(crypto.hmac('sha256', key, `${header}.${payload}`, 'binary'));
	return `${header}.${payload}.${signature}`;
}

/** 실행할 때 넘긴 JWT_SECRET. 없으면 바로 멈춰요. */
export function jwtSecret() {
	const secret = __ENV.JWT_SECRET;
	if (!secret) {
		throw new Error('JWT_SECRET이 필요해요: k6 run -e JWT_SECRET=... (loadtest/.env와 같은 값)');
	}
	return secret;
}
