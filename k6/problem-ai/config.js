import http from 'k6/http'; // k6의 HTTP 통신 모듈 (GET, POST 등 요청용)
import { check } from 'k6';  // 응답 결과(200 OK 등) 성공 여부 검증용 모듈

/**
 * ============================================================================
 * [Problem-AI 도메인 전용 k6 설정 모듈]
 *
 * 📌 대상 API: POST /api/spaces/{spaceId}/problems/ai (AI 기반 문제 자동 생성)
 * 📌 주의: 이 API는 호출마다 실제 LLM(외부 AI) API를 호출합니다.
 *          VU/duration을 무리하게 올리면 실제 비용이 발생하고, LLM 제공사
 *          자체 rate limit(429)에 걸려 우리 서버 문제와 구분이 안 될 수 있습니다.
 *          -> room-submit 테스트보다 VU를 훨씬 낮게 잡습니다.
 * ============================================================================
 */

// 1. [서버 주소] EC2 + Nginx (momogo.kro.kr)
export const BASE_URL = 'http://momogo.kro.kr';

// 2. [테스트 계정] ADMIN 권한 + 공간(Space) 소유 계정
export const ADMIN_USER = {
  email: 'jun@test.com',
  password: '7964a23!', // TODO: 실제 비밀번호로 교체해서 로컬에서만 사용 (커밋 금지)
};

// 3. [테스트 대상] ADMIN_USER 소유 공간/카테고리 UUID
export const SPACE_ID = '07ddf318-4762-45b4-84e6-ec6be69d4f14';
export const CATEGORY_ID = 'e52067a1-8e66-4ec9-a343-927405c3b69d';

/**
 * [JWT 인증 토큰 발급 setup 함수]
 * 💡 백엔드 Spring Security 폼 로그인(/api/auth/sign-in) 규격에 맞춰 JWT AccessToken을 받아옵니다.
 */
export function obtainAuthToken() {
  const loginUrl = `${BASE_URL}/api/auth/sign-in`;

  const formData = `username=${encodeURIComponent(ADMIN_USER.email)}&password=${encodeURIComponent(ADMIN_USER.password)}`;

  const headers = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  };

  const loginRes = http.post(loginUrl, formData, headers);

  const isLoginSuccess = check(loginRes, {
    'Login Successful (HTTP 200)': (r) => r.status === 200,
  });

  if (!isLoginSuccess) {
    console.error(`[Setup 에러] 로그인 실패 (Status: ${loginRes.status}, Body: ${loginRes.body}).`);
    console.error(`=> DB에 ${ADMIN_USER.email} 계정이 존재하는지, ADMIN 권한인지 확인해 주세요!`);
    return 'mock-jwt-access-token-sample';
  }

  const token = loginRes.json('accessToken') || loginRes.headers['Authorization'] || 'mock-jwt-access-token-sample';
  const cleanToken = token.replace('Bearer ', '');

  console.log(`[Setup 성공] 테스트 계정(${ADMIN_USER.email}) 로그인 및 JWT 토큰 발급 완료!`);
  return cleanToken;
}

/**
 * [CSRF 토큰 조회 함수]
 * 💡 SecurityConfig에서 '/api/auth/sign-in'만 CSRF 예외 처리됨. POST/PUT/PATCH/DELETE 요청은
 *    전부 CSRF 토큰(쿠키+헤더 X-XSRF-TOKEN 일치) 없으면 403 Forbidden 처리됨 (JWT 인증과 별개).
 * ⚠️ 로그인 응답(/api/auth/sign-in)이 이미 XSRF-TOKEN 쿠키를 같이 내려주므로,
 *    반드시 obtainAuthToken()을 먼저 호출한 뒤에 이 함수를 호출해야 함.
 *    (별도로 /api/auth/csrf-token GET을 다시 호출하면, 이미 쿠키가 있어 서버가 Set-Cookie를
 *    다시 내려주지 않아 응답 자체에서는 토큰을 못 얻는 함정이 있어 쿠키 jar에서 직접 읽음)
 */
export function obtainCsrfToken() {
  const cookies = http.cookieJar().cookiesForURL(BASE_URL);

  const csrfToken = cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : null;

  if (!csrfToken) {
    console.error('[Setup 에러] CSRF 토큰 쿠키를 찾지 못했습니다. obtainAuthToken()을 먼저 호출했는지 확인하세요.');
  }

  return csrfToken;
}

/**
 * [AI 문제 생성 요청 Payload 생성 함수]
 * ProblemAiCreateRequest 스펙: categoryId(필수), referenceText(필수, 10000자 이하), questionCount(1~10)
 */
export function getProblemAiPayload(questionCount = 3) {
  return JSON.stringify({
    categoryId: CATEGORY_ID,
    referenceText: `세종대왕은 1443년 훈민정음을 창제하여 백성들이 쉽게 글을 배우고 쓸 수 있도록 했다.
당시 한자는 배우기 어려워 일반 백성들이 문자 생활에서 소외되어 있었는데, 세종대왕은 이를 문제로 인식하고
집현전 학자들과 함께 소리 나는 대로 적을 수 있는 표음 문자 체계를 연구했다. 훈민정음은 자음 17자와 모음 11자,
총 28자로 구성되어 있으며 초성, 중성, 종성을 조합해 음절을 표기하는 독창적인 원리를 갖고 있다.
1446년 훈민정음 해례본을 반포하며 그 창제 원리와 사용법을 상세히 기록했다. 오늘날 한글은 배우기 쉽고
표현이 정확한 문자로 세계적으로도 과학적인 문자 체계로 평가받고 있다.`,
    questionCount,
  });
}
