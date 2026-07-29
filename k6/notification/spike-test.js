import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL } from '../config.js';
import { SPACE_ID, STUDENT_USER_IDS, obtainAdminToken, obtainCsrfToken, buildRoomCreatePayload } from './config.js';

/**
 * ============================================================================
 * [도메인: Notification / 기능: 시험방 생성 시 알림 동기 처리 (POST /api/spaces/{spaceId}/rooms)]
 * [3. Spike Test - 급증 대응 테스트]
 *
 * 🎯 테스트 목적:
 *   "개학철/시험 기간 직전, 여러 선생님이 몇 초 만에 한꺼번에 몰려서
 *    시험방을 만드는" 현실적인 급증 상황을 흉내낸다.
 *   급증 구간에서 얼마나 무너지는지, 그리고 급증이 지나간 뒤
 *   응답이 정상으로 회복되는지(혹은 계속 저하된 채로 남는지)를 관찰한다.
 * ============================================================================
 */

const errorRate = new Rate('error_rate');
const roomCreateLatency = new Trend('room_create_latency');

const INVITE_COUNT = 30;

export const options = {
  stages: [
    { duration: '1m', target: 5 },     // 평소 수준
    { duration: '10s', target: 300 },  // 급증!
    { duration: '1m', target: 300 },   // 급증 상태 유지
    { duration: '10s', target: 5 },    // 급하강
    { duration: '2m', target: 5 },     // 회복 관찰 구간
  ],
  thresholds: {
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
}
