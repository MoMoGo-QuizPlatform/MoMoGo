import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, SPACE_ID, obtainAuthToken, obtainCsrfToken, getProblemAiPayload } from './config.js'; // Problem-AI 전용 설정 모듈 참조

/**
 * ============================================================================
 * [도메인: Problem-AI / 기능: AI 기반 문제 자동 생성 (POST /api/spaces/{spaceId}/problems/ai)]
 * [2. Stress Test - 한계 및 임계점(Break Point) 테스트]
 *
 * 🎯 테스트 목적:
 *   - 백엔드 정답 검증용 스레드풀(problemValidationExecutor, max 10)과
 *     동기 LLM 호출 구조상 어느 VU 수준부터 응답이 무너지는지 확인합니다.
 *   - LLM 실호출 비용 때문에 room-submit(VU 200)과 달리 VU 12까지만 단계적으로 올립니다.
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const aiLatency = new Trend('ai_generation_latency');

export const options = {
  stages: [
    { duration: '1m', target: 2 },  // [1단계] VU 2명 (기본 정상 작동 확인)
    { duration: '1m', target: 4 },  // [2단계] VU 4명
    { duration: '1m', target: 6 },  // [3단계] VU 6명
    { duration: '1m', target: 8 },  // [4단계] VU 8명
    { duration: '1m', target: 10 }, // [5단계] VU 10명 (검증 executor 풀 max와 동일 지점)
    { duration: '1m', target: 12 }, // [6단계] VU 12명 (풀 한계 초과 구간)
    { duration: '30s', target: 0 }, // [7단계] 쿨다운
  ],
  thresholds: {
    'error_rate': ['rate<0.3'], // Stress Test 특성상 한계 탐색이 목적이라 30%까지 허용
  },
};

export function setup() {
  const token = obtainAuthToken();
  const csrfToken = obtainCsrfToken();
  return { authToken: token, csrfToken };
}

export default function (data) {
  const targetUrl = `${BASE_URL}/api/spaces/${SPACE_ID}/problems/ai`;

  const payload = getProblemAiPayload(3);

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
    'Server Alive (Not 500 Error)': (r) => r.status !== 500,
  });

  errorRate.add(!isSuccess);
  aiLatency.add(duration);

  sleep(1);
}
