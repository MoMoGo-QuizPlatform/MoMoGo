import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, ROOM_ID, obtainAuthToken, getSampleSubmitPayload } from '../config.js'; // 공통 설정 상위 모듈 참조

/**
 * ============================================================================
 * [도메인: Room / 기능: 답안 일괄 제출 (POST /api/rooms/{roomId}/submit)]
 * [3. Spike Test - 순간 부하 폭증 및 복구력 테스트]
 * 
 * 🎯 테스트 목적:
 *   - "시험 마감 10초 전 순간 150명이 동시 제출" 하는 폭증 상황에서
 *     서버가 다운되지 않고, 부하 해제 후 다시 정상 200 OK로 복구되는지 검증합니다.
 * ============================================================================
 */

// 1. [커스텀 메트릭 변수 선언]
const errorRate = new Rate('error_rate');          // 전체 요청 중 실패한 요청 비율 수집 변수
const submitLatency = new Trend('submit_latency');  // 답안 제출 API 응답 지연시간(ms) 수집 변수

// 2. [k6 실행 시나리오 - 순간 150명 부하 폭증(Spike) 주입 및 검증 기준]
export const options = {
  stages: [
    { duration: '30s', target: 10 },  // [구간 1] 30초간 VU 10명 유지 (평상시 동기화 상태)
    { duration: '10s', target: 150 }, // [구간 2] 🔥 10초 만에 VU 150명으로 급격한 폭증! (Spike 주입)
    { duration: '1m',  target: 150 }, // [구간 3] 1분간 VU 150명 극단 부하 지속 (시험 마감 직전 시뮬레이션)
    { duration: '10s', target: 10 },  // [구간 4] 10초 만에 VU 10명으로 급격히 부하 하강 (Spike 해제)
    { duration: '1m',  target: 10 },  // [구간 5] 1분간 VU 10명 유지하며 서버가 정상 200 OK로 복구되는지 관찰
  ],
  thresholds: {
    'error_rate': ['rate<0.20'], // 순간 폭증 구간 20% 이내 에러 방어 검증
  },
};

// 3. [setup 단계] 로그인 1회 수행 후 JWT 토큰 발급 (30분 유효 토큰)
export function setup() {
  const token = obtainAuthToken(); // ../config.js의 토큰 발급 함수 호출
  return { authToken: token };      // default 실행 함수로 파라미터 전달
}

// 4. [main 단계] 가상 유저(VU)들이 반복 실행하는 메인 테스트 로직
export default function (data) {
  // 호출할 타겟 답안 제출 API URL 조립
  const submitUrl = `${BASE_URL}/api/rooms/${ROOM_ID}/submit`;

  // POST로 전달할 주관식 제출 답안 샘플 JSON 생성
  const payload = getSampleSubmitPayload();

  // HTTP 헤더 설정 (JSON 타입 명시 + JWT Bearer 인증 토큰 주입)
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.authToken}`, // setup에서 넘어온 토큰 적용
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

  // 동시 폭증 상황 유도를 위해 쉬는 시간 최소화 (0.2초 쉬기)
  sleep(0.2);
}
