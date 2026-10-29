// 1차 구조(FRND-3): 30초 하트비트 + 팝오버 30초 폴링.
//   source loadtest/.env
//   k6 run -e JWT_SECRET="$JWT_SECRET" loadtest/polling.js
//   k6 run -e JWT_SECRET="$JWT_SECRET" -e USERS=2000 -e POPOVER=0.3 loadtest/polling.js
//
// 왜 constant-arrival-rate인가:
//   기본(closed) 모델은 VU가 응답을 기다린 뒤 다음 요청을 보내요. 서버가 느려지면 요청도 같이 줄어서 문제가 가려져요.
//   실제 앱은 서버가 느려도 30초마다 보내니까, "초당 몇 건"을 고정하는 open 모델로 흉내 내요.
//   사용자 N명이 30초마다 한 번 → timeUnit 30s에 rate N.
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { accessToken, jwtSecret } from './lib/auth.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:18080';
const USERS = Number(__ENV.USERS || 1000);
// 팝오버를 연 채 두는 사용자 비율. 1이면 모두 열어 둔 최악의 경우예요.
const POPOVER = Number(__ENV.POPOVER || 1);
const DURATION = __ENV.DURATION || '5m';
const POLLERS = Math.max(1, Math.round(USERS * POPOVER));

export const options = {
	scenarios: {
		heartbeat: {
			executor: 'constant-arrival-rate',
			exec: 'heartbeat',
			rate: USERS,
			timeUnit: '30s',
			duration: DURATION,
			preAllocatedVUs: 50,
			maxVUs: 500,
		},
		popover: {
			executor: 'constant-arrival-rate',
			exec: 'popover',
			rate: POLLERS,
			timeUnit: '30s',
			duration: DURATION,
			preAllocatedVUs: 50,
			maxVUs: 500,
		},
	},
	thresholds: {
		http_req_failed: ['rate<0.01'],
		'http_req_duration{scenario:heartbeat}': ['p(95)<200'],
		'http_req_duration{scenario:popover}': ['p(95)<300'],
		// 서버가 따라오지 못해 k6가 보내지 못한 요청. 0이어야 목표 부하를 실제로 준 거예요.
		dropped_iterations: ['count==0'],
	},
};

// 토큰 서명은 사용자마다 한 번만. setup()에서 만든 값은 모든 VU에 복사돼요.
export function setup() {
	const secret = jwtSecret();
	const tokens = [];
	for (let id = 1; id <= USERS; id++) {
		tokens.push(accessToken(id, secret));
	}
	return { tokens };
}

// 이번 차례의 사용자. 반복 번호를 돌려 써서 30초 동안 모든 사용자가 한 번씩 보내요.
function userIndex(count) {
	return exec.scenario.iterationInTest % count;
}

function headers(token) {
	return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
}

export function heartbeat(data) {
	const i = userIndex(USERS);
	const res = http.put(
		`${BASE_URL}/api/v1/presence`,
		JSON.stringify({ status: 'WORKING', icon: 'CODE' }),
		{ headers: headers(data.tokens[i]), tags: { name: 'PUT /presence' } },
	);
	check(res, { 'heartbeat 204': (r) => r.status === 204 });
}

export function popover(data) {
	const i = userIndex(POLLERS);
	const res = http.get(`${BASE_URL}/api/v1/friends`, {
		headers: headers(data.tokens[i]),
		tags: { name: 'GET /friends' },
	});
	check(res, { 'friends 200': (r) => r.status === 200 });
}
