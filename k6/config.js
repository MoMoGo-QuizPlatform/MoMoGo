import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 요청용)
import { check } from 'k6';  // 응답 결과(200 OK 등) 성공 여부 검증용 모듈

/**
 * ============================================================================
 * [MoMoGo k6 공통 환경 설정 모듈]
 * 
 * 📌 팀원 가이드:
 *   - 이 파일은 팀 전체가 공유하는 공통 설정 파일입니다.
 *   - ALB 서버 주소, 테스트용 계정, JWT 인증 토큰 발급 로직이 모여있습니다.
 *   - 백엔드 Spring Security 규격(/api/auth/sign-in)에 맞춘 로그인 토큰 발급 함수입니다.
 * ============================================================================
 */

// 1. [서버 주소] 현재 배포된 AWS ALB(로드밸런서) 퍼블릭 DNS 주소
export const BASE_URL = 'http://momogo-alb-1906718718.ap-northeast-2.elb.amazonaws.com';

// 2. [테스트 시험방] k6tester 계정이 응시 대상자로 등록된 시험방 UUID (1시간 이상 유지!)
export const ROOM_ID = '6dee31a7-8842-43e3-b6ec-1e324bc6b2ef'; 

// 3. [테스트 계정] 사전 작업으로 생성하고 시험방 응시 대상자로 지정해둔 계정 정보
export const TEST_USER = {
  email: 'k6tester@momogo.com',
  password: 'Password123!',
};

/**
 * [JWT 인증 토큰 발급 setup 함수]
 * 💡 백엔드 Spring Security 폼 로그인(/api/auth/sign-in) 규격에 맞춰 JWT AccessToken을 받아옵니다.
 */
export function obtainAuthToken() {
  // 백엔드 실제 로그인 엔드포인트 URL (/api/auth/sign-in)
  const loginUrl = `${BASE_URL}/api/auth/sign-in`;

  // Spring Security formLogin 규격 (username=이메일, password=비밀번호)
  const formData = `username=${encodeURIComponent(TEST_USER.email)}&password=${encodeURIComponent(TEST_USER.password)}`;

  const headers = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  };

  // 백엔드로 Form 로그인 요청 전송
  const loginRes = http.post(loginUrl, formData, headers);

  // 로그인 응답 상태 코드가 200 OK 인지 검증
  const isLoginSuccess = check(loginRes, {
    'Login Successful (HTTP 200)': (r) => r.status === 200,
  });

  if (!isLoginSuccess) {
    console.error(`[Setup 에러] 로그인 실패 (Status: ${loginRes.status}, Body: ${loginRes.body}).`);
    console.error(`=> DB에 TEST_USER 계정(k6tester@momogo.com)이 존재하는지 확인해 주세요!`);
    return 'mock-jwt-access-token-sample';
  }

  // 응답 Body의 accessToken 또는 Authorization 헤더/쿠키에서 토큰 문자열 추출
  const token = loginRes.json('accessToken') || loginRes.headers['Authorization'] || 'mock-jwt-access-token-sample';
  const cleanToken = token.replace('Bearer ', '');

  console.log(`[Setup 성공] 테스트 계정(${TEST_USER.email}) 로그인 및 JWT 토큰 발급 완료!`);
  return cleanToken;
}

/**
 * [시험 답안 제출 Mock Payload 생성 함수]
 */
export function getSampleSubmitPayload() {
  return JSON.stringify({
    answers: [
      {
        roomProblemId: '6b4076dc-f884-41c4-85ec-b1c9f7539617',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 1',
      },
      {
        roomProblemId: 'f11c33b8-bf56-4e2d-b2d5-4cbdea9d49ba',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 2',
      },
      {
        roomProblemId: 'f6c9e267-e6b4-4d71-ba48-f06c17d53ebd',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 3',
      },
      {
        roomProblemId: 'dbfa129d-0bca-473a-9855-9e1176a1ab51',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 4',
      },
      {
        roomProblemId: '4e6a41e1-f78e-4744-8b73-fcbc3ac32174',
        userAnswer: 'k6 부하테스트 주관식 제출 답안 5',
      },
    ],
  });
}
