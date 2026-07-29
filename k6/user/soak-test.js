import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, obtainMultipleAuthTokens } from './config.js';

/**
 * ============================================================================
 * [도메인: User / 기능: 내 프로필 조회 (GET /api/users/me)]
 * [4. Soak Test - 실제 운영 환경 모사 (Multi-User Token Pool)]
 * 
 * 🎯 테스트 목적:
 *   - 25분간 각 VU가 각기 다른 토큰으로 지속 부하를 투입하여
 *     다중 계정 조회 상태에서 Redis 토큰 레지스트리 및 DB Connection Pool 누수 여부를 검증합니다.
 *   - 토큰 유효 시간(30분) 이내인 총 25분 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');
const getMeLatency = new Trend('get_me_latency');

// 2. [k6 테스트 실행 부하 시나리오]
export const options = {
  stages: [
    { duration: '1m',  target: 30 }, // [구간 1] 0~1분: VU 30명까지 도달 (Ramp-up)
    { duration: '23m', target: 30 }, // [구간 2] 1~24분: VU 30명을 23분간 지속 유지 (Soak)
    { duration: '1m',  target: 0 },  // [구간 3] 24~25분: VU 0명으로 감속 및 토큰 만료 전 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<250'], // 장시간 실행에도 95% 요청이 250ms 이하 유지
    'error_rate': ['rate<0.01'],        // 에러율 1% 미만 유지
  },
};

// 3. [setup 단계] 다중 사용자 토큰 풀(Token Pool) 생성
export function setup() {
  const tokens = obtainMultipleAuthTokens(50);
  return { tokens: tokens };
}

// 4. [main 단계] 가상 유저(VU) 식별자(__VU)에 맞춰 각기 다른 유저 토큰으로 요청 실행
let failedLogCount = 0;

export default function (data) {
  const tokenIndex = (__VU - 1) % data.tokens.length;
  const userToken = data.tokens[tokenIndex];

  const url = `${BASE_URL}/api/users/me`;
  const params = {
    headers: {
      'Authorization': `Bearer ${userToken}`,
      'Content-Type': 'application/json',
    },
  };

  const res = http.get(url, params);

  const isSuccess = check(res, {
    'Get My Profile Success (HTTP 200)': (r) => r.status === 200,
  });

  if (!isSuccess && failedLogCount < 5) {
    failedLogCount++;
    console.error(`[요청 실패 샘플 #${failedLogCount}] HTTP ${res.status} | Body: ${res.body}`);
  }

  errorRate.add(!isSuccess);
  getMeLatency.add(res.timings.duration);

  // 지속적 내구성 테스트를 위해 1초 휴식
  sleep(1);
}
