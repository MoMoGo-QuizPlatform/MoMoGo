import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../config.js';
import { SPACE_ID, STUDENT_USER_IDS, obtainAdminToken, obtainCsrfToken, buildRoomCreatePayload } from './config.js';

/**
 * ============================================================================
 * [도메인: Notification / 기능: 시험방 생성 시 알림 동기 처리 (POST /api/spaces/{spaceId}/rooms)]
 * [1. Load Test - 평시 목표 부하 테스트]
 *
 * 🎯 테스트 목적:
 *   선생님 30명 기준(가정), 시험 기간 직전 절반이 동시에 시험방을 만드는
 *   "평상시 피크" 상황(VU 15명)에서도 응답 속도가 괜찮은지 확인한다.
 *   초대 인원은 30명(한 반 규모)으로 고정 - 이번 테스트의 변수는 동시성(VU)뿐.
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const roomCreateLatency = new Trend('room_create_latency');

const INVITE_COUNT = 30;

export const options = {
  stages: [
    { duration: '1m', target: 15 }, // 서서히 15명까지 (Ramp-up)
    { duration: '8m', target: 15 }, // 15명 유지 (Steady State)
    { duration: '1m', target: 0 },  // 서서히 종료 (Ramp-down)
  ],
  thresholds: {
    'http_req_duration': ['p(95)<3000'], // 평시엔 95%가 3초 이내로 들어와야 함 (기준선, 첫 실행 후 조정 가능)
    'error_rate': ['rate<0.01'],
  },
};

export function setup() {
  const token = obtainAdminToken();
  const csrfToken = obtainCsrfToken();
  const invitedUserIds = STUDENT_USER_IDS.slice(0, INVITE_COUNT);
  if (invitedUserIds.length < INVITE_COUNT) {
    console.error(`[Setup 경고] STUDENT_USER_IDS가 ${INVITE_COUNT}명 미만입니다. 시드 데이터를 먼저 준비하세요.`);
  }
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

  // 실제 선생님이 방을 연속으로 만들지는 않으므로 현실적인 간격 유도
  sleep(2);
}
