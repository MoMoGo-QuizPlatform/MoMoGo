import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getStudentEmail, executeLogin, STUDENT_PASSWORD } from './config.js';

/**
 * ============================================================================
 * [도메인: User Login / 기능: 사용자 로그인 (POST /api/auth/sign-in)]
 * [2. Stress Test - 로그인 한계점 탐색 테스트]
 *
 * 🎯 테스트 목적:
 *   - 가상 유저(VU)를 50명 -> 150명 -> 300명으로 단계를 올리며
 *     BCrypt 암호화 연산 및 DB 조회 한계점(Break-point)을 탐색합니다.
 *   - 총 12분 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');
const loginLatency = new Trend('login_latency');

// 2. [k6 테스트 실행 부하 시나리오]
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // [구간 1] 0~2분: VU 50명 유지 (1단계 부하)
    { duration: '2m', target: 150 },  // [구간 2] 2~4분: VU 150명으로 증대
    { duration: '2m', target: 150 },  // [구간 3] 4~6분: VU 150명 유지 (2단계 부하)
    { duration: '2m', target: 300 },  // [구간 4] 6~8분: VU 300명 증대 (3단계 고부하)
    { duration: '2m', target: 300 },  // [구간 5] 8~10분: VU 300명 유지
    { duration: '2m', target: 500 },  // [구간 6] 10~12분: VU 500명 극강 한계 도전 (Extreme Peak)
    { duration: '2m', target: 500 },  // [구간 7] 12~14분: VU 500명 유지
    { duration: '2m', target: 0 },    // [구간 8] 14~16분: VU 0명으로 감속 후 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<10000'], // 500명 극강 스트레스 암호화 연산 고려 95%가 10초 이내
    'error_rate': ['rate<0.10'],          // 에러율 10% 미만 허용
  },
};

// 3. [main 단계] 가상 유저(VU)별 지속적 로그인 실행
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

  // 스트레스 상황 모사를 위해 0.5초 간격 대기
  sleep(0.5);
}
