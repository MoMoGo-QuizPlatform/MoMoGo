import http from 'k6/http';
import { check } from 'k6';

/**
 * ============================================================================
 * [room-submit 전용 k6 환경 설정 모듈 - CSRF 쿠키 자동 연동]
 * ============================================================================
 */

// 1. [서버 주소] 배포된 AWS ALB 퍼블릭 DNS 주소
export const BASE_URL = 'http://momogo-alb-1906718718.ap-northeast-2.elb.amazonaws.com';

// 2. [테스트 시험방] 모모고 부하테스트용 시험방 개설 UUID
export const ROOM_ID = 'ed1b38da-927b-420d-9c6e-2a7a7d5237f0'; 

// 3. [테스트 계정] 사전 작업으로 생성하고 시험방 응시 대상자로 지정해둔 계정 정보
export const TEST_USER = {
  email: 'k6tester@momogo.com',
  password: 'Password123!',
};

/**
 * [JWT 인증 토큰 및 CSRF 토큰 통합 발급 setup 함수]
 */
export function obtainAuthToken() {
  const loginUrl = `${BASE_URL}/api/auth/sign-in`;
  const formData = `username=${encodeURIComponent(TEST_USER.email)}&password=${encodeURIComponent(TEST_USER.password)}`;

  const headers = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  };

  // 백엔드로 Form 로그인 요청 전송
  const loginRes = http.post(loginUrl, formData, headers);

  // 1. 로그인 HTTP 200 OK 검증
  const isLoginSuccess = check(loginRes, {
    'Login Successful (HTTP 200)': (r) => r.status === 200,
  });

  if (!isLoginSuccess) {
    throw new Error(`[Setup 실패] 로그인 실패 (Status: ${loginRes.status}, Body: ${loginRes.body}). 테스트를 중단합니다.`);
  }

  // 2. JWT AccessToken 추출
  let token = null;
  try {
    const jsonBody = loginRes.json();
    if (jsonBody && jsonBody.accessToken) {
      token = jsonBody.accessToken;
    }
  } catch (e) {}

  if (!token) {
    token = loginRes.headers['Authorization'];
  }

  if (!token) {
    throw new Error(`[Setup 실패] accessToken이 누락되었습니다. 테스트를 중단합니다.`);
  }

  // 3. CSRF 쿠키(XSRF-TOKEN) 추출
  let csrfToken = null;
  if (loginRes.cookies && loginRes.cookies['XSRF-TOKEN'] && loginRes.cookies['XSRF-TOKEN'].length > 0) {
    csrfToken = loginRes.cookies['XSRF-TOKEN'][0].value;
  }

  const cleanToken = token.replace('Bearer ', '');
  console.log(`[Setup 성공] 로그인, JWT 토큰 및 CSRF 토큰(${csrfToken}) 발급 완료!`);
  return { authToken: cleanToken, csrfToken: csrfToken };
}

/**
 * [시험 답안 제출 Mock Payload 생성 함수]
 */
export function getSampleSubmitPayload() {
  return JSON.stringify({
    answers: [
      {
        roomProblemId: 'ec697849-a685-45c3-8d60-46273b39b543',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 1',
      },
      {
        roomProblemId: 'f5c49baa-2310-4a67-9b53-23ccaced9097',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 2',
      },
      {
        roomProblemId: 'cf74495e-513f-4007-9e4b-051eea2a81a8',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 3',
      },
      {
        roomProblemId: '91a3f633-b955-4d2c-8d66-94c38e7dd6db',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 4',
      },
      {
        roomProblemId: 'ea823fd8-34cc-44e2-8d1e-f371146b31c5',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 5',
      },
    ],
  });
}
