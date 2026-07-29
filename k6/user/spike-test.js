import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, obtainMultipleAuthTokens } from './config.js';

/**
 * ============================================================================
 * [도메인: User / 기능: 내 프로필 조회 (GET /api/users/me)]
 * [3. Spike Test - 실제 운영 환경 모사 (Multi-User Token Pool)]
 * 
 * 🎯 테스트 목적:
 *   - 가상 유저(VU)들이 서로 다른 토큰을 가지고 10초 만에 20배(10명 -> 200명) 폭증할 때
 *     DB의 다중 Row 조회 상태에서 서버가 다운되지 않고 응답을 복구하는지 검증합니다.
 *   - 토큰 유효 시간(30분) 이내인 총 3분 30초 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');
const getMeLatency = new Trend('get_me_latency');

// 2. [k6 테스트 실행 부하 시나리오]
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // [구간 1] 0~30초: 평시 10명 유지
    { duration: '10s', target: 200 },  // [구간 2] 30~40초: 10초 만에 VU 200명으로 순간 폭증 (Spike Peak)
    { duration: '1m20s', target: 200 },// [구간 3] 40초~2분: 폭증한 VU 200명 유지
    { duration: '10s', target: 10 },   // [구간 4] 2분~2분 10초: 10초 만에 VU 10명으로 급감
    { duration: '1m20s', target: 10 }, // [구간 5] 2분 10초~3분 30초: 평시 복구 상태 관찰
  ],
  thresholds: {
    'error_rate': ['rate<0.10'], // 스파이크 순간의 일부 실패 감안, 전체 에러율 10% 미만
  },
};

// 3. [setup 단계] 다중 사용자 토큰 풀(Token Pool) 생성
export function setup() {
  const tokens = obtainMultipleAuthTokens(200);
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

  // 스파이크 상황이므로 0.2초 간격 대기
  sleep(0.2);
}
