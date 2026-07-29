import type { UserRole } from './user';

export interface SpaceResponse {
  id: string;
  name: string;
  description: string;
  profileImageUrl: string | null;
  createdAt: string;
}

export interface SpaceCreateRequest {
  name: string;
  description: string;
  spacePassword?: string;
  spaceImageUrl?: string;
}

export interface SpaceJoinRequest {
  spacePassword?: string;
}

export interface SpaceUpdateRequest {
  name: string;
  description: string;
  spacePassword?: string;
  spaceImageUrl?: string;
}

export interface SpaceUserRoleUpdateRequest {
  userRole: UserRole;
}

export interface SpaceCursorPaginationResponse<T> {
  values: T[];
  hasNext: boolean;
  nextCursor: string | null;
}
