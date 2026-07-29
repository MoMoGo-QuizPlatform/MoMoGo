import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, ROOM_ID, obtainAuthToken, getSampleSubmitPayload } from './config.js'; // 공통 설정 상위 모듈 참조

/**
 * ============================================================================
 * [도메인: Room / 기능: 답안 일괄 제출 (POST /api/rooms/{roomId}/submit)]
 * [2. Stress Test - 한계 및 임계점(Break Point) 테스트]
 * 
 * 🎯 테스트 목적:
 *   - 부하를 단계적으로 극대화(VUser 10명 -> 200명)하여 **어느 부하 수치에서
 *     서버 응답 속도가 꺾이거나 500 에러/OOM이 발생하는지 한계 지점**을 측정합니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 변수 선언]
const errorRate = new Rate('error_rate');          // 전체 요청 중 실패한 요청 비율 수집 변수
const submitLatency = new Trend('submit_latency');  // 답안 제출 API 응답 지연시간(ms) 수집 변수

// 2. [k6 실행 시나리오 - 단계별 레벨업 부하 주입 및 검증 기준]
export const options = {
  stages: [
    { duration: '1m', target: 10 },  // [1단계] 1분간 VU 10명 유지 (기본 정상 작동 확인)
    { duration: '2m', target: 50 },  // [2단계] 2분간 VU 50명으로 상향 (중간 수준 부하)
    { duration: '2m', target: 100 }, // [3단계] 2분간 VU 100명으로 상향 (1vCPU 한계 진입 시작)
    { duration: '2m', target: 150 }, // [4단계] 2분간 VU 150명으로 상향 (Latency 급증 파악 지점)
    { duration: '2m', target: 200 }, // [5단계] 2분간 VU 200명으로 최상향 (한계 오버 부하)
    { duration: '1m', target: 0 },   // [6단계] 1분간 VU 0명으로 감속하여 서버 쿨다운
  ],
  thresholds: {
    'error_rate': ['rate<0.15'], // Stress Test 특성상 15% 이내 에러율 허용 기준 설정
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

  // 성공 응답 체크 및 500 에러 발생 여부 확인
  const isSuccess = check(res, {
    'Submit Success (200/201)': (r) => r.status === 200 || r.status === 201, // 200/201 성공 여부
    'Server Alive (Not 500 Error)': (r) => r.status !== 500,               // 500 서버 장애 발생 여부
  });

  // 커스텀 지표에 수집 데이터 저장
  errorRate.add(!isSuccess);   // 실패 시 에러 비율 카운터 증가
  submitLatency.add(duration); // 지연시간 합산

  // 가상 유저 간 최소 휴식 시간 (1초 간격)
  sleep(1);
}

/**
 * [k6 테스트 완료 후 자동 HTML 리포트 생성 함수]
 */
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export function handleSummary(data) {
  return {
    'k6/results/stress-test-report.html': htmlReport(data),
  };
}
