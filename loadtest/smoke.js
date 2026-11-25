// 가장 작은 k6 스크립트. 서버가 떠 있는지, k6가 도는지만 봐요.
//   k6 run loadtest/smoke.js
//   k6 run -e BASE_URL=http://localhost:18080 loadtest/smoke.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
	vus: 2,            // 가상 사용자 2명이
	duration: '10s',   // 10초 동안 기본 함수를 반복해요
	thresholds: {
		http_req_failed: ['rate<0.01'],   // 실패 1% 미만
		http_req_duration: ['p(95)<200'], // 95%가 200ms 안에
	},
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// VU 하나가 반복해서 도는 부분이에요. 한 번 = iteration 하나.
export default function () {
	const res = http.get(`${BASE_URL}/api/v1/app/version`);
	check(res, {
		'200 OK': (r) => r.status === 200,
		'최신 버전이 있음': (r) => r.json('latest') !== undefined,
	});
	sleep(1);
}
