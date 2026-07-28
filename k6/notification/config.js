import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config.js';

/**
 * ============================================================================
 * [notification 도메인 k6 설정]
 *
 * 📌 무엇을 증명하는 테스트인가:
 *   POST /api/spaces/{spaceId}/rooms (시험방 생성) API를 두드리지만,
 *   실제로 측정하려는 건 그 안에서 동기적으로 실행되는
 *   NotificationEventListener.handleRoomCreated의 알림 생성 반복 처리 비용이다.
 *   (@Async가 없어서 초대 인원수만큼 DB insert를 순서대로 끝내야 응답이 감)
 *
 * ⚠️ 아래 SPACE_ID / ADMIN_USER / STUDENT_USER_IDS는 아직 시드 데이터가
 *    준비되지 않아 플레이스홀더 상태. 시드 스크립트 실행 후 실제 값으로 교체할 것.
 *    CATEGORY_ID는 배포 DB에 이미 존재하는 '테스트' 카테고리로 확정값.
 *
 * ⚠️ CSRF 주의: 로그인(/api/auth/sign-in)만 예외이고, 그 외 POST/PATCH/PUT/DELETE는
 *    전부 CSRF 토큰이 필요함 (SecurityConfig 확인). obtainCsrfToken()으로 받은 값을
 *    'X-XSRF-TOKEN' 헤더 + 'Cookie: XSRF-TOKEN=...' 헤더 둘 다에 실어 보내야 통과됨.
 * ============================================================================
 */

// 관리자 계정 (기존에 이미 존재하던 계정을 재사용 - "테스트웅" 공간의 ADMIN)
export const ADMIN_USER = {
  email: 'test123@test.com',
  password: 'test1234!',
};

// ADMIN_USER가 이미 ADMIN으로 소속된 "테스트웅" 공간 (배포 DB 기존 데이터, 재사용)
export const SPACE_ID = 'bd909c59-195d-4468-b73d-5afa9853e40a';

// 배포 DB(tbl_problem_category)에 존재하는 카테고리 - 확정값
export const CATEGORY_ID = '34c2b0c2-deb4-4b5b-a27a-7aeeb1ca4496'; // '테스트' 카테고리

// [공용 테스트 픽스처 - 다른 도메인도 재사용 가능하도록]
// k6-student-001@test.com ~ k6-student-200@test.com (비밀번호 test1234! 공통)
// 회원가입 API로 생성 후 SQL로 SPACE_ID 공간에 일괄 배정한 200개 계정
// (같은 유저가 여러 방에 동시에 초대돼도 무방하므로, 동시성 테스트에서도 이 풀 하나만 재사용하면 됨)
export const STUDENT_USER_IDS = [
  'ddd4d4e7-fdf8-4d97-998a-3010a0174b02',
  '41b124e3-f959-48f4-8f97-5764473d7ca1',
  'e12a153c-490a-423a-bcee-c5adc8fe1074',
  '2c82ebcb-a917-4b54-b540-ac4db7a7198c',
  'ca888a2f-a085-4f1f-a987-6800b2766db1',
  '9449427b-77a8-41c4-b9a5-ad59b1fcaebb',
  'a52eb04c-99ad-4938-980d-1cbdd031ab29',
  '5025a8bb-e5c9-4a14-970a-6405e9ae7561',
  '3fd60a68-d790-4374-a700-799be77552dd',
  'ae1c8941-33fc-4957-9677-030f11ebd4c9',
  '8b418a17-4716-42b9-87ec-58078fe4f002',
  '9c9e0974-d1bf-4a2f-88e0-da1faad42b90',
  '176e5792-22ff-4ff5-89f1-c163e0d80e77',
  'aacda85e-aa44-4adb-90e0-6d5084913ba6',
  'fd6b9236-83dc-40c8-a0a7-80a91d0b0b49',
  'd705b0d4-8544-485e-80a3-12417e0a7239',
  '278f1664-c90a-4277-a3f6-3abbdb3d8223',
  'c10a741d-66c1-4e01-87f9-6ef57c467d82',
  '8d981f52-09d7-4be3-819d-da4eec0a876d',
  '9ac7f64c-022b-410a-990a-e2031914a125',
  'ee604650-8329-4ddf-b4e8-0a959072b28a',
  '6e13e711-9a55-471a-af18-c8e2c57c22bf',
  'e67ae924-7ccf-4d1e-a728-c8087f3ad7b2',
  'e110a8a3-4903-4dae-99fc-3e74f4ecebc4',
  '1e2276db-2fcc-466e-b373-ea537e37b62e',
  'e739a48e-8c30-4e08-89c4-e28bae874c03',
  'b242d874-b493-4f9d-8846-3c3cdb530653',
  'afcf133a-9ce5-41eb-8dab-acab80df55b3',
  '841f5e36-f647-49d5-9c7c-1a955e9c0fb6',
  'd1487f7d-6ad4-4a18-ac33-45064951d611',
  'd7950c35-536e-4bd3-a848-653387c3f274',
  'a934bca6-ed82-44e3-904e-f617eff1c267',
  'bd8b6152-1ed3-4399-9738-156f452235c9',
  '50fc9aee-e65c-484a-9893-aa9933721980',
  'b584e69f-e68c-4932-93a0-783058567ad0',
  'ff5bc877-1e97-4442-a550-bac0fb9ffa8d',
  '0b627239-f272-4636-92bb-1d7ae158e39f',
  'ad641db8-49eb-46b5-bc19-cd7c03c097dd',
  '10e50f1f-fe7a-4bb3-b63d-fd89a852bd13',
  'aa45ee55-868d-476b-874b-96f8bf097143',
  '1f546470-ccff-4194-9cb4-c583f4a935ef',
  '9f2f89c9-0e91-4547-a4b3-d04fbba90283',
  'adac7b62-0c4c-4f81-abd8-fabe6cb13137',
  '0646b52a-fe26-4fc2-b46d-ae903f39bef7',
  'ba40530c-9aaf-4bcf-9c8f-205c91cf64bc',
  '38842719-007b-4ef1-b16a-3d9ab6cc4c15',
  '43ba974a-3d0a-4c7e-a21e-18fb5c8cf0ab',
  'dd9899ef-e368-4210-81eb-8e31a5f1c637',
  '136f0822-ebd3-4aaa-9ad7-fec60a5d3176',
  'bcef8d8f-c157-4073-b500-9c78f12bdb37',
  '303007a6-bfcc-49b0-9e12-1fac930c5ef6',
  '81610abb-935f-46be-aff3-7bb2395102cf',
  '7aaf7a23-0543-4e01-a105-2b0e1b541d40',
  'f57abde0-150d-4179-8500-0e19009dcc1e',
  '8e70b561-f9bc-4457-8f63-a45df81baaf5',
  '94a24c99-922c-402d-a8f9-31d50dd18b75',
  '287c8a42-1824-4d6a-a0ca-365ba137e7e5',
  'f4dda900-f22c-4b30-8e35-b5f178187ae4',
  '2e36724c-ac49-4b29-8ee8-0b24511a4945',
  'ac36305c-79e2-4d61-b087-0f3e9c0ea49c',
  'fe9683c3-927b-409d-9ab1-8402c7057c28',
  '0293f3d2-b8b8-4661-94e9-593e8f71042e',
  '6dfc06fa-4796-4001-b2bc-2ef80eaeb13d',
  '6ec3c98e-0c5d-4416-b5dc-fc222a92e383',
  'cdc06b25-8858-414f-91f4-c655a38fbfdd',
  'baafec06-2403-4b19-889d-eb0134231c0a',
  '874af5e0-f21d-417d-906c-2d68af59491c',
  '39487403-bd71-4a12-99a3-3ded519229ae',
  '550a2ae1-9d30-49d3-8497-4b1f0bb1ca9e',
  'e39a641d-3d08-4bd3-8014-88acbd8c7e2e',
  'f789b1c4-b56a-46e4-aa54-ffb7897a47a3',
  'a1b173fd-dc90-47ba-8911-1b0e2f2cd987',
  '2b5c11ce-c414-4c01-827d-dc90ea1d20c5',
  '29eaee86-523c-4120-8cdc-21f567013938',
  '1a16a620-0891-47bf-b929-e02510b81537',
  '7df73efe-43f3-4898-b881-87894caa62ef',
  '0810d6dd-e46a-48d4-a8e3-65207ba420c1',
  '7e7376ee-b20d-4788-84f8-fe9c020895c7',
  'ccc7092e-098a-46a4-a480-26d576320d11',
  '19c0288d-e940-4614-98ab-808d8781bf79',
  'fd010548-d609-4c24-bb2f-da328d5378e0',
  '081b70e0-5d60-405a-a4b7-cb84bb63b001',
  '94681bae-c038-4dd3-bf5b-af4822f95f65',
  '48f0c83a-1958-4569-ad73-1dc0e05d9c9f',
  '1e360612-ba7c-4471-a27c-9d69d55e5785',
  'c9787ec6-a37b-41c1-a873-88c6be8ee2b6',
  '4d578121-4cf0-4c75-9bad-bca6251877da',
  'ac4efba6-79f8-4951-9049-8af857daf047',
  '8b650ca6-1880-4ecc-b8e0-c6866e3b7101',
  'dbb21936-e751-4625-8293-2eefbd123d18',
  '61921d1c-f272-4b6c-aab9-1f3ac69c18bc',
  'fb687a58-878c-4cc7-9e60-7f46edc9b653',
  '3616593b-dc65-4012-a698-626c61e7e6e8',
  '0250d6df-b6ef-4cf2-868f-648b25fe5bff',
  '80b9ad39-e79f-49a0-8463-278b2574e313',
  '24bdd82f-7286-4da0-ab33-f27f818160aa',
  '82fb0787-1507-4fca-a21c-a41e08319942',
  'deb58afb-0086-4374-b8f5-ed29dc1c1770',
  '68d90b40-4318-454b-b92a-1db2ce34df31',
  '1527d75e-ee1a-4487-aefb-7af1e8c3166d',
  'ad0e2d3c-3952-44f4-95b5-8bcced7b9f84',
  'aeb7f1e8-dbd9-493a-8c9c-60df81f66084',
  'be06b811-3a50-4d0a-af8a-c7590fe4cc41',
  'a1be2ff3-5fcc-44cd-ae6f-c0b8a6ab3068',
  'a44fc3f1-2266-4fa1-bf69-d9aef1ac0f4e',
  'c87dd805-012c-4ab7-8cf2-5af3f8913ffb',
  '73756ab9-5be8-417c-b199-a23d03a4e355',
  '75b5c8ad-e14c-47cb-bab6-fb393bf107ad',
  '42b03c13-5fd1-439a-89a7-0591f3991355',
  'c25506f2-4951-4893-859e-7778f60b364e',
  '848b9fa9-437b-4cd8-be22-b925b5e4922b',
  '9df13e64-196a-4268-bb2e-2af3ed625b34',
  '8f8a3d99-702d-450c-9c58-d0e79448ae8a',
  '320b0b11-6e53-4641-9406-5dc9035b930f',
  '6cad81fa-7cff-491b-aa08-49902c66194a',
  '9ee2292d-c786-4add-9930-9138dd4ccf53',
  'cf158857-b1e6-41c9-addb-95d637145bf5',
  '2509f04c-1a07-47a2-8132-479f6fadb886',
  '969ffc58-5759-4bff-a19c-fd8d999f8c29',
  '30d0d304-a0fc-4091-a34a-24a39f0192ab',
  '1c9f510e-f1c3-4235-a140-ec38948b5bb5',
  '9cdc4674-15b8-47c3-99af-e020a1389b2f',
  'a1e8778c-89ab-4073-8c0d-4f9d6ddf4319',
  '4eb5b465-9618-40f6-801c-fe6876a426a7',
  '17da51f1-eb6c-436f-8c70-f01623fa8ad0',
  'adda0de2-e93b-48c0-9ec3-7a9653a3744e',
  '65469752-2bc3-4077-a675-ef1f15544d41',
  '2b38d822-ea88-415a-a798-2eccdc047646',
  'cbd14aa0-f2f4-48c6-94b1-f5b7ff04601f',
  'd61664be-badf-40a7-a501-a8991b6e4ae2',
  'b503e08c-6584-4684-b733-5247af6f66aa',
  '125bf4d7-7dbf-4e0c-a155-328ca780ca12',
  '005bbaa1-3391-48ec-8805-3c65d08d93e5',
  '9c18ad4d-2b99-4614-8b6c-b854e95e13fa',
  '6975e26f-002c-471c-ba67-5bd1e456b151',
  'c10546bc-18b3-4ecf-ba87-0b00b524e0d8',
  'ae22cbf8-d263-4ba0-ae37-0d9497e3a573',
  'fbd8bb02-abaf-43c8-bd6c-6587c7ab357b',
  '161c14ea-9730-422c-a29e-8de0677b53ef',
  '91815a2d-2fa4-47d3-a073-0bd3d8b2237e',
  '55fcc6bf-51c9-4e5d-ace1-8efd5022fa68',
  '37718467-f2d5-41b7-a529-11ed110351fb',
  '944c132c-e713-413e-8476-8301c3860a2b',
  '7098d744-9a30-4f5d-bcfe-7d53459593ac',
  'b0110472-c38d-4041-b957-91dd3f7af01a',
  '617a2ed1-47b4-4ddd-bf7b-0045db18e4c1',
  '4f8c57ff-a9c8-483c-8098-eb9b1300174e',
  '20daf1b6-e977-4a92-a546-f5a5bbf69c05',
  '4d4a4d08-8f4b-4dba-b965-d4a3ed76493c',
  '6400cc95-f98b-48bb-a2f2-b64a8de74ba6',
  '7d8ecec1-66f2-4d64-82c4-3531ea36b1a3',
  '4fc48b6d-8d27-457a-ad24-11ef56ccfa56',
  '9a2909ff-d421-4ce6-89b2-f1bbf819b3cf',
  '8518438d-511a-48f3-b4e3-c678f86ca53a',
  'b0c957c0-199e-41f7-ad6f-50f04aaec145',
  'ea31e5fc-b8b5-4ac7-a683-f5a035749997',
  'b91a541e-99ac-4b6b-90e4-70d2d2b09f6d',
  '9d87e321-947a-4a98-a99c-bcfd66f40e17',
  '2fb16dca-6717-4208-99d9-c09065ca1aad',
  '4369fe10-575d-40fe-a473-289ab096f111',
  'd8cac084-9d98-4fd2-b732-dd7dc8a6420a',
  'b74d42bb-6850-44bc-b64c-a0f5f99f602d',
  '30cb0929-c129-4fb8-af93-b4724c933783',
  '75cb10c2-7590-4182-9747-b6175b622db2',
  '20819585-e781-4ac6-acba-97ac24a32b14',
  '0d1ebecd-0863-460b-a3ad-e71f70bfff4d',
  '5a13464f-fafa-41d9-a2da-b1e249e808e1',
  '991131c5-6b54-4e24-8988-299cf976ddcc',
  'e49e308a-5756-412e-b39b-6078d8a56f53',
  '5f1b150f-4b9f-4395-9475-521ae64715ec',
  '025b0124-f39a-4fb9-aa70-3c9eb5f30684',
  '77ca7546-6c0d-4231-9b9e-1a037aec7059',
  'f6e69a02-bb27-46e4-ac40-370343411959',
  'aa220ca8-5edd-46be-9ae3-94722106f875',
  'a6be3bd1-f8c0-4a8a-98cd-70a49b066969',
  'bb1fa8ce-fb81-4a8f-bdb0-cb4a7b5ae497',
  '77795b52-b6de-4533-ae5e-454b17c2db3e',
  'd7911207-d774-4e91-a52e-8e470578d175',
  '5458d56f-1e12-4651-ac4b-3e23c0d156b6',
  '3459611b-e09c-4595-b8b6-711119c7b076',
  '73b5a44a-eb1f-48e2-8952-7886354aa3c5',
  'daf7208d-4074-45b0-ae5c-4053fbfe7d1a',
  '0343256f-db57-4a76-81b2-9ca0de46d842',
  'e581e0b2-ae12-4b4b-b11c-559893a830e3',
  '64231a26-666d-4b53-95e6-63a3135f5e77',
  '7bb43ffb-cdc4-47d3-a0b9-3e167311601f',
  'd5c40a0a-1f6c-4077-9485-724078d0d005',
  'de8aa551-706c-4590-b01a-4de198128197',
  'fa602b61-bc27-45f8-89f0-8bec05f594fb',
  '29d19e31-ea93-4ba3-913b-6ef848278a05',
  'eb99e3b7-fa02-40a2-a914-f8b8a21b0ec7',
  '63f0d6c4-1063-41d2-86d4-806ca694a392',
  '58fc500b-db1c-4a66-a5c6-dcf9d91819f2',
  '4f6948dc-ec98-4a60-8b3f-6702c95d6fa6',
  'dca5484e-fd23-4478-bd28-0de64192d16d',
  '72dd2d50-03ee-4fbb-bba1-98992b08237c',
  '766ade80-c049-40a9-99df-7465cf2d02a6',
  'dbd97c2e-1c04-40e7-8381-e1eae8683167',
  '9dc5d50b-1cbd-4adb-8fd6-bdaec5641fc6',
  '42dfd8b5-b5d7-42b1-b42c-845791d9da39',
];

/**
 * [관리자 로그인 함수]
 * 상위 공용 config.js의 obtainAuthToken()은 TEST_USER(k6tester) 계정으로 고정되어 있어
 * 우리 도메인의 ADMIN_USER로는 재사용할 수 없음. 그래서 별도로 둠
 * (공용 파일을 파라미터화하는 방안은 팀과 논의 후 결정)
 */
export function obtainAdminToken() {
  const loginUrl = `${BASE_URL}/api/auth/sign-in`;
  const formData = `username=${encodeURIComponent(ADMIN_USER.email)}&password=${encodeURIComponent(ADMIN_USER.password)}`;
  const headers = {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  };

  const loginRes = http.post(loginUrl, formData, headers);

  const isLoginSuccess = check(loginRes, {
    'Admin Login Successful (HTTP 200)': (r) => r.status === 200,
  });

  if (!isLoginSuccess) {
    console.error(`[Setup 에러] 관리자 로그인 실패 (Status: ${loginRes.status}, Body: ${loginRes.body}).`);
    console.error(`=> 배포 DB에 ADMIN_USER 계정(${ADMIN_USER.email})이 존재하고 SPACE_ID 공간의 ADMIN인지 확인해 주세요!`);
    return 'mock-jwt-access-token-sample';
  }

  const token = loginRes.json('accessToken') || 'mock-jwt-access-token-sample';
  console.log(`[Setup 성공] 관리자 계정(${ADMIN_USER.email}) 로그인 및 JWT 토큰 발급 완료!`);
  return token.replace('Bearer ', '');
}

/**
 * [CSRF 토큰 발급 함수]
 * GET /api/auth/csrf-token 호출 시 서버가 Set-Cookie로 XSRF-TOKEN을 내려줌.
 * 이 값을 그대로 X-XSRF-TOKEN 헤더 + Cookie 헤더에 실어 보내야 POST 요청이 통과됨.
 * (setup()의 쿠키 컨텍스트가 각 VU의 default() 실행과 공유되지 않으므로,
 *  토큰 값 자체를 데이터로 리턴해서 매 요청마다 수동으로 헤더에 실어야 함)
 */
export function obtainCsrfToken() {
  const res = http.get(`${BASE_URL}/api/auth/csrf-token`);
  const cookie = res.cookies['XSRF-TOKEN'];

  if (!cookie || cookie.length === 0) {
    console.error('[Setup 에러] CSRF 토큰 쿠키를 받지 못했습니다.');
    return null;
  }

  console.log('[Setup 성공] CSRF 토큰 발급 완료!');
  return cookie[0].value;
}

/**
 * [시험방 생성 요청 Payload 생성 함수]
 * @param {string[]} userIds - 초대할 응시 대상자 UUID 목록 (이 배열 길이가 알림 반복 횟수를 결정하는 핵심 변수)
 */
export function buildRoomCreatePayload(userIds) {
  const now = Date.now();
  const testStartAt = new Date(now + 5 * 60 * 1000).toISOString();  // 5분 뒤 (FutureOrPresent 제약 충족)
  const testEndAt = new Date(now + 60 * 60 * 1000).toISOString();   // 1시간 뒤 (Future 제약 충족)

  return JSON.stringify({
    name: `k6 시험방 ${__VU}-${__ITER}`,
    description: 'k6 부하테스트용 자동 생성 시험방 (알림 병목 검증)',
    testStartAt: testStartAt,
    testEndAt: testEndAt,
    userIds: userIds,
    // 문제 내용은 이 테스트와 무관하므로 고정된 mock 값 재사용
    // (AI 자동출제 단계는 의도적으로 생략 - 외부 AI 호출 지연시간이 섞이면
    //  "알림 처리 때문에 느린 건지 AI 때문에 느린 건지" 구분이 안 됨)
    problems: [
      {
        categoryId: CATEGORY_ID,
        name: 'k6 부하테스트용 문제',
        content: '부하테스트를 위한 더미 문제 내용입니다.',
        explanation: '해설(선택값)',
        correctAnswer: '정답 더미값',
        problemOrder: 1,
      },
    ],
  });
}
