import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과(200 OK 등) 검증 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, ROOM_ID, obtainAuthToken, getSampleSubmitPayload } from './config.js';

/**
 * ============================================================================
 * [도메인: Room / 기능: 답안 일괄 제출 (POST /api/rooms/{roomId}/submit)]
 * [1. Load Test - 평시 목표 부하 테스트]
 * 
 * 🎯 테스트 목적:
 *   - 예상 가능한 정상 사용량(VUser 30명)일 때 시스템의 응답 속도와 처리량이
 *     목표 SLA(p95 Latency < 500ms)를 만족하는지 검증합니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 변수 선언]
const errorRate = new Rate('error_rate');          // 전체 요청 중 실패한 요청 비율 수집 변수
const submitLatency = new Trend('submit_latency');  // 답안 제출 API 응답 지연시간(ms) 수집 변수

// 2. [k6 실행 시나리오 및 통과 기준(Thresholds) 설정]
export const options = {
  stages: [
    { duration: '30s', target: 30 }, // [구간 1] 0~30초: 가상 유저(VU)를 30명까지 서서히 상향 (Ramp-up)
    { duration: '4m',  target: 30 }, // [구간 2] 30초~4분30초: VU 30명을 유지하며 평시 부하 측정 (Steady State)
    { duration: '30s', target: 0 },  // [구간 3] 4분30초~5분: VU를 0명으로 감속하여 안전하게 완료 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // [성공 조건 1] 95%의 요청이 500ms(0.5초) 이내 들어올 것
    'error_rate': ['rate<0.01'],        // [성공 조건 2] 전체 요청 중 에러 비율이 1% 미만(0.01)일 것
  },
};

// 3. [setup 단계] 테스트 실행 직전 로그인 API 및 CSRF 토큰을 발급받음
export function setup() {
  const authData = obtainAuthToken(); 
  return authData;     
}

// 4. [main 단계] 30명의 가상 유저(VU)들이 반복 실행하는 실제 테스트 메인 로직
export default function (data) {
  // 호출할 답안 제출 API 전용 URL 생성
  const submitUrl = `${BASE_URL}/api/rooms/${ROOM_ID}/submit`;

  // POST로 전달할 주관식 제출 답안 샘플 JSON 생성
  const payload = getSampleSubmitPayload();

  // HTTP 헤더 설정 (Content-Type 및 Bearer JWT 인증 토큰 / CSRF 토큰 및 쿠키 적용)
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.authToken}`, 
      'X-XSRF-TOKEN': data.csrfToken,
      'X-CSRF-TOKEN': data.csrfToken,
      'Cookie': `XSRF-TOKEN=${data.csrfToken}`,
    },
  };

  const startTime = Date.now(); // HTTP 요청 직전 시간(ms) 기록

  // 백엔드로 POST 답안 제출 요청 전송
  const res = http.post(submitUrl, payload, params);

  const duration = Date.now() - startTime; // HTTP 요청 응답에 걸린 소요 시간(ms) 계산

  // 성공 여부 검증 (응답 상태 코드가 200 OK 또는 201 Created 인지 체크)
  const isSuccess = check(res, {
    'Submit Answer Success (200/201)': (r) => r.status === 200 || r.status === 201,
  });

  // 커스텀 지표에 수집 데이터 저장
  errorRate.add(!isSuccess);   // 성공 시 0, 실패 시 1 저장하여 에러 비율 누적
  submitLatency.add(duration); // 소요된 지연시간(ms) 저장하여 p95 연산에 활용

  // 실제 사용자들이 고민 후 제출하는 현실적인 간격 유도 (1초 휴식)
  sleep(1);
}

/**
 * [k6 테스트 완료 후 자동 HTML 리포트 생성 함수]
 */
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export function handleSummary(data) {
  return {
    'k6/results/load-test-report.html': htmlReport(data),
  };
}
