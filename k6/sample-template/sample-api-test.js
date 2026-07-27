import http from 'k6/http'; // k6 HTTP 요청 전송 모듈
import { check, sleep } from 'k6'; // 성공 검증(check) 및 요청 간격 쉬기(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 지표 측정 모듈
import { BASE_URL, obtainAuthToken } from '../config.js'; // 상위 공통 설정 파일에서 URL 및 토큰 함수 가져오기

/**
 * ============================================================================
 * [팀원 복사용 샘플 템플릿 - API 부하 테스트]
 * 
 * 💡 이 파일을 복사하여 본인의 도메인 폴더(예: k6/my-domain/test.js)로 옮긴 후
 *    아래 1~3번 표시된 주석 라인만 본인 API 사양에 맞게 수정하여 사용하세요!
 * ============================================================================
 */

// [커스텀 메트릭] 에러율과 응답 지연시간을 수집할 변수 선언
const errorRate = new Rate('error_rate');     // 에러율 (실패 횟수 / 전체 횟수)
const apiLatency = new Trend('api_latency');  // 응답 지연시간(ms) 수집

// [k6 시나리오 옵션 설정]
export const options = {
  stages: [
    { duration: '30s', target: 30 }, // 30초 동안 가상 유저(VU)를 30명까지 올림 (Ramp-up)
    { duration: '3m',  target: 30 }, // 3분 동안 VU 30명을 지속 유지하며 테스트 (Steady State)
    { duration: '30s', target: 0 },  // 30초 동안 VU 0명으로 내려서 종료 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95%의 요청이 500ms(0.5초) 이내에 들어와야 성공
    'error_rate': ['rate<0.01'],        // 에러 발생 비율이 1% 미만이어야 성공
  },
};

// [1. setup 단계] 테스트 시작 전 공통 JWT 토큰을 로그인 API로부터 1회 받아옴
export function setup() {
  const token = obtainAuthToken(); // config.js의 토큰 발급 함수 호출
  return { authToken: token };      // 메인 함수(default)로 토큰 전달
}

// [2. main 단계] 가상 유저(VU)들이 반복 실행할 메인 테스트 로직
export default function (data) {
  
  // ✏️ [수정 포인트 1] 테스트할 본인 도메인의 API 엔드포인트 URL 지정
  const targetUrl = `${BASE_URL}/api/spaces`; // 예: /api/spaces, /api/users/me 등

  // ✏️ [수정 포인트 2] POST/PUT 요청 시 보낼 JSON 바디 데이터 (GET 요청 시 필요 없음)
  const payload = JSON.stringify({
    // name: '샘플 데이터',
    // description: '설명 예시',
  });

  // ✏️ [수정 포인트 3] HTTP 헤더 설정 (JWT 인증 토큰 및 JSON 전송)
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.authToken}`, // setup에서 받은 토큰 자동 주입
    },
  };

  const startTime = Date.now(); // 요청 전 시작 시각 기록

  // ✏️ [수정 포인트 4] HTTP 메서드 선택 (http.get 또는 http.post/put/del)
  const res = http.get(targetUrl, params); 
  // POST의 경우: const res = http.post(targetUrl, payload, params);

  const duration = Date.now() - startTime; // 요청 처리 소요 시간(ms) 계산

  // ✏️ [수정 포인트 5] 성공 여부 판정 규칙 (예: 200 OK 인지 검증)
  const isSuccess = check(res, {
    'API Status 200 OK': (r) => r.status === 200,
  });

  // 메트릭 집계에 결과 합산
  errorRate.add(!isSuccess);   // 실패 시 에러율 카운트 증가
  apiLatency.add(duration);    // 응답 지연시간 합산

  // 실제 사용자들의 행동 인터벌 간격 유도 (1초 휴식)
  sleep(1);
}
