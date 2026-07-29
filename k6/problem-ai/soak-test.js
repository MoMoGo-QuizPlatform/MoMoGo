import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 전송용)
import { check, sleep } from 'k6'; // 응답 결과 검증(check) 및 요청 간 쉼(sleep) 모듈
import { Rate, Trend } from 'k6/metrics'; // 에러율(Rate) 및 응답시간(Trend) 커스텀 측정 모듈
import { BASE_URL, SPACE_ID, obtainAuthToken, obtainCsrfToken, getProblemAiPayload } from './config.js'; // Problem-AI 전용 설정 모듈 참조

/**
 * ============================================================================
 * [도메인: Problem-AI / 기능: AI 기반 문제 자동 생성 (POST /api/spaces/{spaceId}/problems/ai)]
 * [4. Soak Test - 내구성 및 리소스 누수(Leak) 테스트]
 *
 * 🎯 테스트 목적:
 *   - 낮은 VU로 장시간 반복 호출 시 검증용 스레드풀(problemValidationExecutor),
 *     LLM 클라이언트 커넥션, 메모리에 누수/고갈이 없는지 확인합니다.
 *   - room-submit(VU 20 / 25분)과 달리 이 API는 호출마다 실제 LLM 비용이 발생하므로
 *     VU 2 / 10분으로 축소해서 진행합니다. (JWT 만료 30분 이내 범위)
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const aiLatency = new Trend('ai_generation_latency');

export const options = {
  stages: [
    { duration: '1m',  target: 2 }, // [구간 1] 1분간 VU 2명으로 서서히 진입 (Ramp-up)
    { duration: '10m', target: 2 }, // [구간 2] 10분간 VU 2명으로 장시간 지속 부하 주입 (Soak Duration)
    { duration: '1m',  target: 0 }, // [구간 3] 1분간 VU 0명으로 감속 후 안전 종료 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<15000'], // 12분간 지속 실행 시에도 p95 응답시간 15초 이내 유지
    'error_rate': ['rate<0.05'],          // 전체 에러 비율이 5% 미만일 것
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

  // 관리자의 현실적인 반복 생성 간격 유도 (5초 휴식)
  sleep(5);
}
