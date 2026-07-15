import { request, setAccessToken } from './api';
import type { UserCreateRequest, UserResponse } from '../types/user';

export interface LoginResponse {
  user: UserResponse;
  accessToken: string;
}

// CSRF 토큰 초기 획득 API
export async function getCsrfToken(): Promise<void> {
  await request<void>('/api/auth/csrf-token', { method: 'GET' });
}

// 일반 폼 로그인 API (Spring Security formLogin)
export async function login(email: string, password: string): Promise<LoginResponse> {
  // CSRF 토큰 활성화를 위해 먼저 CSRF 조회 진행
  await getCsrfToken();

  const formData = new URLSearchParams();
  formData.append('username', email);
  formData.append('password', password);

  const response = await request<LoginResponse>('/api/auth/sign-in', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    body: formData.toString(),
  });

  // 전역 메모리에 Access Token 주입
  if (response && response.accessToken) {
    setAccessToken(response.accessToken);
  }

  return response;
}

// 회원가입 API
export async function signUp(data: UserCreateRequest): Promise<UserResponse> {
  return request<UserResponse>('/api/users', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// 토큰 갱신 API
export async function refresh(): Promise<LoginResponse> {
  const response = await request<LoginResponse>('/api/auth/refresh', {
    method: 'POST',
  });

  if (response && response.accessToken) {
    setAccessToken(response.accessToken);
  }

  return response;
}

// 로그아웃 API
export async function logout(): Promise<void> {
  await request<void>('/api/auth/sign-out', {
    method: 'POST',
  });
  setAccessToken(null);
}

// 임시 비밀번호 찾기 API
export async function findPassword(email: string): Promise<void> {
  await request<void>('/api/auth/password-find', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

// 유예 계정 복구 API
export async function restoreUser(password: string): Promise<void> {
  await request<void>('/api/auth/restore', {
    method: 'POST',
    body: JSON.stringify({ password }),
  });
}
