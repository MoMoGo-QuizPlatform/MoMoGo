import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getStudentEmail, executeLogin, STUDENT_PASSWORD } from './config.js';

/**
 * ============================================================================
 * [도메인: User Login / 기능: 사용자 로그인 (POST /api/auth/sign-in)]
 * [1. Load Test - 평시 부하 테스트]
 *
 * 🎯 테스트 목적:
 *   - 가상 유저(VU 30명)가 DB의 200개 유저 계정으로 동시 로그인할 때
 *     Spring Security + BCrypt(Cost 10) 암호화 검증 및 JWT 발급 처리 속도가
 *     목표 SLA(p95 Latency < 500ms)를 만족하는지 검증합니다.
 *   - 총 5분 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');               // 실패한 로그인 비율 수집 (0~1)
const loginLatency = new Trend('login_latency');         // 로그인 API 응답 소요시간(ms) 수집

// 2. [k6 테스트 실행 시나리오 및 통과 기준(Thresholds)]
export const options = {
  stages: [
    { duration: '30s', target: 50 }, // [구간 1] 0~30초: 가상 유저(VU) 50명까지 서서히 증가 (Ramp-up)
    { duration: '4m',  target: 50 }, // [구간 2] 30초~4분 30초: 50명 유지하며 피크 로그인 부하 측정 (Steady State)
    { duration: '30s', target: 0 },  // [구간 3] 4분 30초~5분: 0명으로 서서히 감소 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2500'], // [성공 조건 1] 50명 동시 암호화 로그인 고려 95% 요청이 2.5초(2500ms) 이내일 것
    'error_rate': ['rate<0.01'],         // [성공 조건 2] 전체 에러율이 1% 미만일 것
  },
};

// 3. [main 단계] 가상 유저(VU)별로 고유 계정으로 지속적 로그인 실행
let failedLogCount = 0;

export default function () {
  // VU 식별자(__VU)와 반복 횟수(__ITER)를 활용해 200개 DB 계정을 고르게 선택
  const studentEmail = getStudentEmail(__VU, __ITER);

  // 로그인 API 호출
  const res = executeLogin(studentEmail, STUDENT_PASSWORD);

  // 성공 여부 검증 (HTTP 200 OK & accessToken 응답 확인)
  const isSuccess = check(res, {
    'Login Success (HTTP 200)': (r) => r.status === 200,
    'AccessToken Returned': (r) => r.body && (r.body.includes('accessToken') || r.headers['Authorization']),
  });

  // 에러 발생 시 디버깅 샘플 로그 (최대 5회 콘솔 출력)
  if (!isSuccess && failedLogCount < 5) {
    failedLogCount++;
    console.error(`[로그인 실패 샘플 #${failedLogCount}] 계정: ${studentEmail} | HTTP ${res.status} | Body: ${res.body}`);
  }

  // 커스텀 메트릭 누적 (k6 내장 res.timings.duration 활용)
  errorRate.add(!isSuccess);
  loginLatency.add(res.timings.duration);

  // 로그인 후 사용자 대기 간격 (1초 휴식)
  sleep(1);
}
