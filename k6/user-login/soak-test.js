import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { getStudentEmail, executeLogin, STUDENT_PASSWORD } from './config.js';

/**
 * ============================================================================
 * [도메인: User Login / 기능: 사용자 로그인 (POST /api/auth/sign-in)]
 * [4. Soak Test - 장시간 로그인 내구성 및 메모리 누수 검증]
 *
 * 🎯 테스트 목적:
 *   - 25분간 각 VU가 지속적으로 로그인을 반복 수행할 때
 *     InMemory / Redis 세션 registry 및 DB Connection Pool 누수가 발생하는지 검증합니다.
 *   - 총 25분 동안 진행됩니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 수집 변수]
const errorRate = new Rate('error_rate');
const loginLatency = new Trend('login_latency');

// 2. [k6 테스트 실행 부하 시나리오]
export const options = {
  stages: [
    { duration: '1m',  target: 50 }, // [구간 1] 0~1분: VU 50명까지 도달 (Ramp-up)
    { duration: '23m', target: 50 }, // [구간 2] 1~24분: VU 50명을 23분간 지속 유지 (Soak)
    { duration: '1m',  target: 0 },  // [구간 3] 24~25분: VU 0명으로 감속 후 안전 종료
  ],
  thresholds: {
    'http_req_duration': ['p(95)<3000'], // 50명 25분 장시간 로그인 고려 95% 요청이 3.0초 이내
    'error_rate': ['rate<0.01'],        // 에러율 1% 미만 유지
  },
};

// 3. [main 단계] 지속적 장시간 로그인 실행
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

  // 지속적 내구성 테스트를 위해 1초 휴식
  sleep(1);
}
