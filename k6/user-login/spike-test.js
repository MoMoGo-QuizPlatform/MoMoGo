import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getStudentEmail, executeLogin, STUDENT_PASSWORD } from './config.js';

/**
 * ============================================================================
 * [도메인: User Login / 기능: 사용자 로그인 (POST /api/auth/sign-in)]
 * [3. Spike Test - 로그인 순간 폭증 테스트]
 *
 * 🎯 테스트 목적:
 *   - 시험 시작 직전 등 10초 만에 20배(10명 -> 200명)의 사용자가 동시 로그인할 때
 *     서버 CPU 및 DB 커넥션 락이 튀지 않고 복구되는지 검증합니다.
 *   - 총 3분 30초 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');
const loginLatency = new Trend('login_latency');

// 2. [k6 테스트 실행 부하 시나리오]
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // [구간 1] 0~30초: 평시 10명 유지
    { duration: '10s', target: 200 },  // [구간 2] 30~40초: 10초 만에 200명 순간 폭증 (Spike Peak)
    { duration: '1m20s', target: 200 },// [구간 3] 40초~2분: 폭증한 200명 로그인 동시 유지
    { duration: '10s', target: 10 },   // [구간 4] 2분~2분 10초: 10초 만에 10명으로 급감
    { duration: '1m20s', target: 10 }, // [구간 5] 2분 10초~3분 30초: 평시 복구 상태 관찰
  ],
  thresholds: {
    'http_req_duration': ['p(95)<8000'], // 200명 순간 일제 로그인 고려 95% 요청 8초 이내
    'error_rate': ['rate<0.10'],         // 스파이크 순간 일시적 에러 감안, 전체 에러율 10% 미만
  },
};

// 3. [main 단계] 가상 유저(VU)별 동시 폭증 로그인 실행
let failedLogCount = 0;

export default function () {
  const studentEmail = getStudentEmail(__VU, __ITER);
  const res = executeLogin(studentEmail, STUDENT_PASSWORD);

  const isSuccess = check(res, {
    'Login Success (HTTP 200)': (r) => r.status === 200,
    'AccessToken Returned': (r) => r.body && (r.body.includes('accessToken') || r.headers['Authorization']),
  });

  if (!isSuccess && failedLogCount < 5) {
    failedLogCount++;
    console.error(`[로그인 실패 샘플 #${failedLogCount}] 계정: ${studentEmail} | HTTP ${res.status} | Body: ${res.body}`);
  }

  errorRate.add(!isSuccess);
  loginLatency.add(res.timings.duration);

  // 스파이크 상황이므로 0.2초 간격 대기
  sleep(0.2);
}
