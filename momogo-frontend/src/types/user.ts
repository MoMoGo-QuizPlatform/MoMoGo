export type UserRole = 'USER' | 'ADMIN' | 'SUPER_ADMIN';

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  banned: boolean;
  profileImageUrl: string | null;
  createdAt: string;
}

export interface UserCreateRequest {
  name: string;
  email: string;
  password?: string;
}

export interface UserUpdateRequest {
  name?: string;
  password?: string;
}

export interface UserBannedRequest {
  banned: boolean;
}
