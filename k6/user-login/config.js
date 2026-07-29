import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER } from '../config.js';

/**
 * ============================================================================
 * [k6/user-login 전용 공통 환경 설정 모듈]
 *
 * 🎯 모듈 목적:
 *   - 백엔드 순수 로그인 API (POST /api/auth/sign-in) 부하 테스트 전용 설정
 *   - DB에 생성된 200개의 학생 계정 (k6-student-001@test.com ~ k6-student-200@test.com)
 *   - 학생 계정 비밀번호: test1234!
 *   - Spring Security FormLogin 규격 (username=...&password=...) 지원
 * ============================================================================
 */

export { BASE_URL, TEST_USER };

// 학생 계정 200개 공통 비밀번호 및 계정 수 설정
export const STUDENT_PASSWORD = 'test1234!';
export const TOTAL_STUDENT_COUNT = 200;

/**
 * [가상 유저(VU) 번호 및 반복 횟수에 대응하는 학생 계정 이메일 생성]
 * @param {number} vuIndex - 가상 유저 식별자 (__VU)
 * @param {number} iterIndex - 반복 횟수 (__ITER)
 */
export function getStudentEmail(vuIndex, iterIndex = 0) {
  // VU 번호와 Iteration을 조합하여 200개 계정을 고르게 순환 분배
  const accountNum = ((vuIndex + iterIndex - 1) % TOTAL_STUDENT_COUNT) + 1;
  const studentNum = String(accountNum).padStart(3, '0');
  return `k6-student-${studentNum}@test.com`;
}

/**
 * [단일 로그인 API 전송 헬퍼 함수]
 * @param {string} email - 로그인 이메일
 * @param {string} password - 비밀번호
 */
export function executeLogin(email, password = STUDENT_PASSWORD) {
  const loginUrl = `${BASE_URL}/api/auth/sign-in`;
  const formData = `username=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`;

  const params = {
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
  };

  return http.post(loginUrl, formData, params);
}
