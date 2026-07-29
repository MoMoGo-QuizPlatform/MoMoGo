import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, obtainMultipleAuthTokens } from './config.js';

/**
 * ============================================================================
 * [도메인: User / 기능: 내 프로필 조회 (GET /api/users/me)]
 * [2. Stress Test - 실제 운영 환경 모사 (Multi-User Token Pool)]
 * 
 * 🎯 테스트 목적:
 *   - 가상 유저(VU)를 50명 -> 150명 -> 300명으로 단계를 올리며
 *     각 VU가 서로 다른 유저 토큰으로 DB의 다량의 유저 데이터(Row)를 동시 조회할 때
 *     인증 필터 및 DB 커넥션 한계점(Break-point)을 탐색합니다.
 *   - 토큰 유효 시간(30분) 이내인 총 12분 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');
const getMeLatency = new Trend('get_me_latency');

// 2. [k6 테스트 실행 부하 시나리오]
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // [구간 1] 0~2분: VU 50명 유지 (1단계 부하)
    { duration: '2m', target: 150 },  // [구간 2] 2~4분: VU 150명으로 증대
    { duration: '2m', target: 150 },  // [구간 3] 4~6분: VU 150명 유지 (2단계 부하)
    { duration: '2m', target: 300 },  // [구간 4] 6~8분: VU 300명으로 한계 도전
    { duration: '2m', target: 300 },  // [구간 5] 8~10분: VU 300명 유지 (최대 부하)
    { duration: '2m', target: 0 },    // [구간 6] 10~12분: VU 0명으로 감속 후 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<400'], // 스트레스 환경 고려 95%가 400ms 이내
    'error_rate': ['rate<0.05'],        // 에러율 5% 미만 허용
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

  // 고부하 상황을 모사하기 위해 0.5초 간격 대기
  sleep(0.5);
}
