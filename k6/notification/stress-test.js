import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../config.js';
import { SPACE_ID, STUDENT_USER_IDS, obtainAdminToken, obtainCsrfToken, buildRoomCreatePayload } from './config.js';

/**
 * ============================================================================
 * [도메인: Notification / 기능: 시험방 생성 시 알림 동기 처리 (POST /api/spaces/{spaceId}/rooms)]
 * [2. Stress Test - 한계점 탐색 테스트]
 *
 * 🎯 테스트 목적:
 *   동시 요청이 계단식으로 늘어날 때, 몇 건부터 응답이 무너지고
 *   에러가 발생하기 시작하는지(시스템의 실제 한계)를 찾는다.
 *   초대 인원은 30명으로 고정, 오직 동시 사용자(VU)만 계속 늘림.
 *   일부러 현실적인 규모(선생님 30명 가정)를 훨씬 넘어서까지 밀어붙인다.
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const roomCreateLatency = new Trend('room_create_latency');

const INVITE_COUNT = 30;

export const options = {
  stages: [
    { duration: '2m', target: 20 },
    { duration: '2m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '2m', target: 200 },
    { duration: '2m', target: 400 },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    // 한계점을 찾는 게 목적이라 임계값 실패 자체가 유의미한 관측 결과.
    // 에러율만 보수적으로 감시 (응답시간은 의도적으로 하드 기준 없음)
    'error_rate': ['rate<0.5'],
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

  // sleep 없음 - 한계까지 최대한 밀어붙이는 게 목적
}
