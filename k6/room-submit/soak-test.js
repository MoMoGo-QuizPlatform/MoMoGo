import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, ROOM_ID, obtainAuthToken, getSampleSubmitPayload } from './config.js'; // 공통 설정 상위 모듈 참조

/**
 * ============================================================================
 * [도메인: Room / 기능: 답안 일괄 제출 (POST /api/rooms/{roomId}/submit)]
 * [4. Soak Test - 내구성 및 메모리 누수(Memory Leak) 테스트]
 * 
 * 🎯 테스트 목적:
 *   - JWT 토큰 만료 30분 이내 범위인 25분 동안 일정 부하(VU 20명)를 지속하여
 *     장시간 실행 시 메모리 누수, GC 누적, DB 커넥션 유실이 없는지 검증합니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 변수 선언]
const errorRate = new Rate('error_rate');          // 전체 요청 중 실패한 요청 비율 수집 변수
const submitLatency = new Trend('submit_latency');  // 답안 제출 API 응답 지연시간(ms) 수집 변수

// 2. [k6 실행 시나리오 - 25분 장시간 내구성 테스트 및 검증 기준]
export const options = {
  stages: [
    { duration: '1m',  target: 20 }, // [구간 1] 1분간 VU 20명으로 서서히 진입 (Ramp-up)
    { duration: '23m', target: 20 }, // [구간 2] 23분간 VU 20명으로 장시간 지속 부하 주입 (Soak Duration)
    { duration: '1m',  target: 0 },  // [구간 3] 1분간 VU 0명으로 감속 후 안전 종료 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // [성공 조건 1] 25분간 지속 실행 시에도 p95 응답시간 < 500ms 유지
    'error_rate': ['rate<0.01'],        // [성공 조건 2] 전체 에러 비율이 1% 미만일 것
  },
};

// 3. [setup 단계] 테스트 실행 직전 로그인 API 및 CSRF 토큰을 발급받음
export function setup() {
  const authData = obtainAuthToken(); 
  return authData;     
}

// 4. [main 단계] 가상 유저(VU)들이 반복 실행하는 실제 테스트 메인 로직
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

  const startTime = Date.now(); // 요청 직전 시각(ms) 기록

  // 백엔드로 POST 답안 제출 요청 전송
  const res = http.post(submitUrl, payload, params);

  const duration = Date.now() - startTime; // 응답 처리 소요 시간(ms) 계산

  // 성공 응답 상태 체크 (200 OK 또는 201 Created)
  const isSuccess = check(res, {
    'Submit Success (200/201)': (r) => r.status === 200 || r.status === 201,
  });

  // 커스텀 지표에 수집 데이터 저장
  errorRate.add(!isSuccess);   // 실패 시 에러 비율 카운터 증가
  submitLatency.add(duration); // 지연시간 합산

  // 가상 유저 간 현실적인 요청 간격 휴식 (2초 휴식)
  sleep(2);
}

/**
 * [k6 테스트 완료 후 자동 HTML 리포트 생성 함수]
 */
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export function handleSummary(data) {
  return {
    'k6/results/soak-test-report.html': htmlReport(data),
  };
}
