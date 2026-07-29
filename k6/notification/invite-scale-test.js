import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config.js';
import { SPACE_ID, STUDENT_USER_IDS, obtainAdminToken, obtainCsrfToken, buildRoomCreatePayload } from './config.js';

/**
 * ============================================================================
 * [도메인: Notification / 원인 증명 실험 - 표준 4종(load/stress/spike/soak)과 별개]
 *
 * 🎯 목적:
 *   동시성 없이(VU 1명, 단발 실행) 초대 인원수만 1→10→50→100→200으로
 *   늘려가며, 응답시간이 인원수에 비례해서(선형으로) 늘어나는지 확인한다.
 *   이게 확인되면 "@Async 없는 동기 반복 처리가 병목"이라는 가설의
 *   직접적인 증거가 된다. (표준 4종은 이 병목을 전제로 한 부하 테스트)
 *
 * 📊 실행 후 콘솔의 CSV 로그를 복사해서 스프레드시트에 붙여넣고
 *    그래프(X=초대 인원, Y=응답시간)를 그려서 선형성을 확인할 것.
 * ============================================================================
 */

const INVITE_LEVELS = [1, 10, 50, 100, 200];
const REPEATS_PER_LEVEL = 3; // 레벨당 3회씩 반복해서 노이즈를 줄임

export const options = {
  scenarios: {
    invite_scale: {
      executor: 'shared-iterations',
      vus: 1, // 동시성 없음 - 순수하게 인원수 변수만 관찰
      iterations: INVITE_LEVELS.length * REPEATS_PER_LEVEL,
      maxDuration: '10m',
    },
  },
};

export function setup() {
  const token = obtainAdminToken();
  const csrfToken = obtainCsrfToken();
  console.log('CSV_HEADER,invite_count,duration_ms,status');
  return { authToken: token, csrfToken: csrfToken };
}

export default function (data) {
  const levelIndex = Math.floor(__ITER / REPEATS_PER_LEVEL);
  const inviteCount = INVITE_LEVELS[levelIndex];
  const invitedUserIds = STUDENT_USER_IDS.slice(0, inviteCount);

  const createUrl = `${BASE_URL}/api/spaces/${SPACE_ID}/rooms`;
  const payload = buildRoomCreatePayload(invitedUserIds);

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${data.authToken}`,
      'X-XSRF-TOKEN': data.csrfToken,
      'Cookie': `XSRF-TOKEN=${data.csrfToken}`,
    },
    tags: { invite_count: String(inviteCount) },
  };

  const startTime = Date.now();
  const res = http.post(createUrl, payload, params);
  const duration = Date.now() - startTime;

  check(res, {
    'Room Create Success (201)': (r) => r.status === 201,
  });

  // CSV 형식 로그 - 복사해서 스프레드시트에 붙여넣어 그래프로 확인
  console.log(`CSV,${inviteCount},${duration},${res.status}`);
}
