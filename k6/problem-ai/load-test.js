import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, SPACE_ID, obtainAuthToken, obtainCsrfToken, getProblemAiPayload } from './config.js'; // Problem-AI 전용 설정 모듈 참조

/**
 * ============================================================================
 * [도메인: Problem-AI / 기능: AI 기반 문제 자동 생성 (POST /api/spaces/{spaceId}/problems/ai)]
 * [1. Load Test - 평시 목표 부하 테스트]
 *
 * 🎯 테스트 목적:
 *   - 관리자 2~3명이 동시에 AI 문제 생성을 사용하는 평시 상황에서
 *     응답 속도/에러율이 안정적인지 baseline을 확인합니다.
 *   - 이 API는 동기 LLM 호출 구조(캐시/큐 없음)라 room-submit보다 훨씬 낮은 VU로 진행합니다.
 * ============================================================================
 */

const errorRate = new Rate('error_rate');            // 전체 요청 중 실패한 요청 비율 수집 변수
const aiLatency = new Trend('ai_generation_latency'); // AI 문제 생성 API 응답 지연시간(ms) 수집 변수

export const options = {
  stages: [
    { duration: '30s', target: 3 }, // [구간 1] 30초간 VU 3명까지 서서히 상향 (Ramp-up)
    { duration: '3m',  target: 3 }, // [구간 2] 3분간 VU 3명 유지하며 평시 부하 측정 (Steady State)
    { duration: '30s', target: 0 }, // [구간 3] 30초간 VU 0명으로 감속하여 종료 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<15000'], // LLM 호출 특성상 15초 기준 (baseline 확보 후 재조정)
    'error_rate': ['rate<0.05'],          // 에러 발생 비율이 5% 미만이어야 성공
  },
};

// [setup 단계] 테스트 시작 전 ADMIN 계정으로 JWT 토큰 + CSRF 토큰 1회 발급
export function setup() {
  const token = obtainAuthToken();
  const csrfToken = obtainCsrfToken();
  return { authToken: token, csrfToken };
}

// [main 단계] 가상 유저(VU)들이 반복 실행할 메인 테스트 로직
export default function (data) {
  const targetUrl = `${BASE_URL}/api/spaces/${SPACE_ID}/problems/ai`;

  const payload = getProblemAiPayload(3); // 문항 3개 생성 요청

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.authToken}`,
      'X-XSRF-TOKEN': data.csrfToken,
      'Cookie': `XSRF-TOKEN=${data.csrfToken}`,
    },
  };

  const startTime = Date.now();

  const res = http.post(targetUrl, payload, params);

  const duration = Date.now() - startTime;

  const isSuccess = check(res, {
    'AI Problem Generation Success (201)': (r) => r.status === 201,
  });

  if (!isSuccess) {
    console.error(`[요청 실패] Status: ${res.status}, Body: ${res.body}`);
  }

  errorRate.add(!isSuccess);
  aiLatency.add(duration);

  // 관리자가 결과 확인 후 다시 생성하는 현실적인 간격 유도 (3초 휴식)
  sleep(3);
}
