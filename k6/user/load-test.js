import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, obtainMultipleAuthTokens } from './config.js';

/**
 * ============================================================================
 * [도메인: User / 기능: 내 프로필 조회 (GET /api/users/me)]
 * [1. Load Test - 실제 운영 환경 모사 (Multi-User Token Pool)]
 * 
 * 🎯 테스트 목적:
 *   - 가상 유저(VU)가 각기 다른 유저 토큰(Token Pool)을 사용하여
 *     실제 운영 환경처럼 DB의 다양한 유저 데이터(Row)를 대규모 동시 조회할 때
 *     인증 필터 오버헤드 및 응답속도가 목표 SLA(p95 Latency < 200ms)를 만족하는지 검증합니다.
 *   - 토큰 유효 시간(30분) 이내인 총 5분 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');               // 실패한 요청 비율 수집 (0~1)
const getMeLatency = new Trend('get_me_latency');       // API 응답 소요시간(ms) 수집

// 2. [k6 테스트 실행 부하 시나리오 및 통과 기준(Thresholds)]
export const options = {
  stages: [
    { duration: '30s', target: 30 }, // [구간 1] 0~30초: 가상 유저(VU) 30명까지 서서히 증가 (Ramp-up)
    { duration: '4m',  target: 30 }, // [구간 2] 30초~4분 30초: 30명 유지하며 평시 부하 측정 (Steady State)
    { duration: '30s', target: 0 },  // [구간 3] 4분 30초~5분: 0명으로 서서히 감소 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<200'], // [성공 조건 1] 95%의 요청이 200ms 이내 들어올 것
    'error_rate': ['rate<0.01'],        // [성공 조건 2] 전체 에러율이 1% 미만일 것
  },
};

// 3. [setup 단계] 실제 운영 모사를 위한 다중 사용자 토큰 풀(Token Pool) 10개 생성
export function setup() {
  const tokens = obtainMultipleAuthTokens(50);
  return { tokens: tokens };
}

// 4. [main 단계] 가상 유저(VU) 식별자(__VU)에 맞춰 각기 다른 유저 토큰으로 요청 실행
let failedLogCount = 0;

export default function (data) {
  // __VU (Virtual User ID: 1, 2, 3...) 별로 서로 다른 토큰 할당
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

  // HTTP 응답 코드가 200 OK 인지 검증
  const isSuccess = check(res, {
    'Get My Profile Success (HTTP 200)': (r) => r.status === 200,
  });

  // 에러 원인 파악을 위한 실패 응답 샘플 로그 (최대 5회까지 콘솔 출력)
  if (!isSuccess && failedLogCount < 5) {
    failedLogCount++;
    console.error(`[요청 실패 샘플 #${failedLogCount}] HTTP ${res.status} | Body: ${res.body}`);
  }

  // 커스텀 지표 누적 (k6 내장 res.timings.duration 활용)
  errorRate.add(!isSuccess);
  getMeLatency.add(res.timings.duration);

  // 일반 사용자의 대기 간격 (1초 휴식)
  sleep(1);
}
