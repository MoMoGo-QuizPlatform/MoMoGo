import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER, obtainAuthToken as obtainSingleToken } from '../config.js';

/**
 * ============================================================================
 * [k6/user 전용 공통 환경 설정 모듈]
 * 
 * 📌 다중 사용자(Multi-User) 실제 운영 트래픽 모사 지원:
 *   - setup() 시 여러 유저 계정의 JWT 토큰 풀(Token Pool)을 발급받아
 *     각 가상 유저(VU)가 서로 다른 유저 토큰으로 요청하도록 지원합니다.
 * ============================================================================
 */

export { BASE_URL, TEST_USER };

// 학생 계정들(k6-student-XXX@test.com) 생성 시 설정된 비밀번호 (test1234!)
export const STUDENT_PASSWORD = 'test1234!';

/**
 * [특정 이메일/비밀번호에 대한 JWT 인증 토큰 발급 함수]
 */
export function obtainAuthTokenForUser(email, password = STUDENT_PASSWORD) {
  const loginUrl = `${BASE_URL}/api/auth/sign-in`;
  const formData = `username=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`;

  const headers = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  };

  const loginRes = http.post(loginUrl, formData, headers);

  if (loginRes.status !== 200) {
    console.error(`[학생 계정 로그인 실패] 계정: ${email} | Status: ${loginRes.status} | Body: ${loginRes.body}`);
    return null;
  }

  const token = loginRes.json('accessToken') || loginRes.headers['Authorization'] || '';
  return token.replace('Bearer ', '');
}

/**
 * [다중 사용자 JWT 인증 토큰 풀(Token Pool) 발급 함수]
 * 💡 실제 운영 환경처럼 여러 가상 유저(VU)가 각기 다른 유저 계정의 토큰을 갖도록
 *    실제 DB 유저 계정(k6-student-001@test.com ~ k6-student-200@test.com) 토큰을 받아옵니다.
 */
export function obtainMultipleAuthTokens(userCount = 10) {
  const tokens = [];

  // 1. 기본 계정 로그인
  const defaultToken = obtainSingleToken();
  if (defaultToken && defaultToken !== 'mock-jwt-access-token-sample') {
    tokens.push(defaultToken);
  }

  // 2. 실제 DB 등록된 학생 계정 로그인 시도
  let failedConsecutiveCount = 0;
  for (let i = 1; i <= userCount; i++) {
    const studentNum = String(i).padStart(3, '0');
    const token = obtainAuthTokenForUser(`k6-student-${studentNum}@test.com`, STUDENT_PASSWORD);

    if (token) {
      tokens.push(token);
      failedConsecutiveCount = 0;
    } else {
      failedConsecutiveCount++;
      if (failedConsecutiveCount >= 3) {
        break;
      }
    }
  }

  if (tokens.length === 0) {
    console.error(`[Setup 에러] 로그인 실패. DB 계정 존재 여부를 확인해 주세요!`);
    tokens.push('mock-jwt-access-token-sample');
  } else {
    console.log(`[Setup 성공] 총 ${tokens.length}개의 가상 유저 토큰(Token Pool) 준비 완료!`);
    if (tokens.length === 1) {
      console.warn(`[Setup 경고] 추가 학생 계정 로그인 실패로 인해 기본 계정(${TEST_USER.email}) 1개로 실행됩니다.`);
    }
  }

  return tokens;
}
