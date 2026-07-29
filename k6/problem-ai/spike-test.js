import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, SPACE_ID, obtainAuthToken, obtainCsrfToken, getProblemAiPayload } from './config.js'; // Problem-AI 전용 설정 모듈 참조

/**
 * ============================================================================
 * [도메인: Problem-AI / 기능: AI 기반 문제 자동 생성 (POST /api/spaces/{spaceId}/problems/ai)]
 * [3. Spike Test - 순간 부하 폭증 및 복구력 테스트]
 *
 * 🎯 테스트 목적:
 *   - "여러 관리자가 동시에 AI 생성 버튼을 누르는" 순간 폭증 상황에서
 *     서버가 다운되지 않고 부하 해제 후 정상 201로 복구되는지 검증합니다.
 *   - LLM 실호출 비용 때문에 room-submit(VU 150)과 달리 VU 10 수준으로 폭증을 재현합니다.
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const aiLatency = new Trend('ai_generation_latency');

export const options = {
  stages: [
    { duration: '20s', target: 1 },  // [구간 1] 20초간 VU 1명 유지 (평상시 상태)
    { duration: '10s', target: 10 }, // [구간 2] 🔥 10초 만에 VU 10명으로 급격한 폭증 (Spike 주입)
    { duration: '30s', target: 10 }, // [구간 3] 30초간 VU 10명 유지 (동시 클릭 폭증 시뮬레이션)
    { duration: '10s', target: 1 },  // [구간 4] 10초 만에 VU 1명으로 급격히 부하 하강 (Spike 해제)
    { duration: '30s', target: 1 },  // [구간 5] 30초간 VU 1명 유지하며 정상 201로 복구되는지 관찰
  ],
  thresholds: {
    'error_rate': ['rate<0.3'], // 순간 폭증 구간 30% 이내 에러 방어 검증
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
  });

  errorRate.add(!isSuccess);
  aiLatency.add(duration);

  sleep(0.5);
}
