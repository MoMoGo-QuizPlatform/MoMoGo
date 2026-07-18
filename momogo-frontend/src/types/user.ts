export const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/;
export const PASSWORD_INVALID_MESSAGE = '새 비밀번호는 8~20자이며, 영문, 숫자, 특수문자(@$!%*#?&)를 적어도 하나씩 포함해야 합니다.';

export type UserRole = 'USER' | 'ADMIN' | 'SUPER_ADMIN';

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  banned: boolean;
  profileImageUrl: string | null;
  social?: 'NONE' | 'GOOGLE' | 'KAKAO';
  createdAt: string;
}

export interface UserCreateRequest {
  name: string;
  email: string;
  password?: string;
}

export interface UserUpdateRequest {
  name?: string;
  currentPassword?: string | null;
  newPassword?: string | null;
  removeProfileImage?: boolean;
}

export interface UserBannedRequest {
  banned: boolean;
}
