import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../config.js';
import { SPACE_ID, STUDENT_USER_IDS, obtainAdminToken, obtainCsrfToken, buildRoomCreatePayload } from './config.js';

/**
 * ============================================================================
 * [도메인: Notification / 기능: 시험방 생성 시 알림 동기 처리 (POST /api/spaces/{spaceId}/rooms)]
 * [4. Soak Test - 장시간 안정성 테스트]
 *
 * 🎯 테스트 목적:
 *   낮은 부하를 오래 유지했을 때 시간이 지날수록 응답이 서서히 느려지거나
 *   (누수), 에러율이 조금씩 증가하는지 확인한다. 알림 반복 처리 자체보다는
 *   시스템 전반의 장시간 안정성(DB 커넥션/메모리 누수 등)을 보는 테스트.
 *
 * ⚠️ 총 실행시간을 25분으로 캡: JWT AccessToken 유효시간이 30분이라
 *    그 이상 돌리면 토큰 만료로 401이 나기 시작해 결과가 오염됨.
 *    (진짜 soak은 보통 몇 시간이지만, 발표/검증 목적으로 압축)
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const roomCreateLatency = new Trend('room_create_latency');

const INVITE_COUNT = 30;

export const options = {
  stages: [
    { duration: '2m', target: 10 },
    { duration: '21m', target: 10 }, // 낮은 부하를 길게 유지 (2+21+2 = 25분 총합)
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    'error_rate': ['rate<0.01'],
  },
};

export function setup() {
  const token = obtainAdminToken();
  const csrfToken = obtainCsrfToken();
  const invitedUserIds = STUDENT_USER_IDS.slice(0, INVITE_COUNT);
  return { authToken: token, csrfToken: csrfToken, invitedUserIds: invitedUserIds };
}

export default function (data) {
  const createUrl = `${BASE_URL}/api/spaces/${SPACE_ID}/rooms`;
  const payload = buildRoomCreatePayload(data.invitedUserIds);

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.authToken}`,
      'X-XSRF-TOKEN': data.csrfToken,
      'Cookie': `XSRF-TOKEN=${data.csrfToken}`,
    },
  };

  const startTime = Date.now();
  const res = http.post(createUrl, payload, params);
  const duration = Date.now() - startTime;

  const isSuccess = check(res, {
    'Room Create Success (201)': (r) => r.status === 201,
  });

  errorRate.add(!isSuccess);
  roomCreateLatency.add(duration);

  sleep(3); // 낮은 부하를 오래 유지하는 게 목적이므로 여유 있게 쉼
}
