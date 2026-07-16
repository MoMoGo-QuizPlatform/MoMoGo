package com.momogo.core.domain.space.service;

import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.dto.response.SpaceCursorPaginationResponse;
import com.momogo.core.domain.space.dto.response.SpaceResponse;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.user.entity.enums.UserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpaceService {

  // 공간 개설 (개설자는 ADMIN으로 Role 변경)
  Space createSpace(UUID userId, SpaceCreateRequest request);

  // 비밀번호 기반 공간 가입
  void joinSpace(UUID userId, UUID spaceId, String password);

  // 내가 현재 소속된 공간 정보 조회
  Optional<SpaceResponse> getMySpace(UUID userId);

  // 내가 가입하지 않은 전체 공간 목록 조회
  SpaceCursorPaginationResponse<Space> getUnjoinedSpacesByCursor(UUID userId, UUID lastSpaceId, OffsetDateTime lastCreatedAt, int size);

  // 공간 정보 수정 (ADMIN 권한 검증)
  Space updateSpace(UUID userId, UUID spaceId, SpaceUpdateRequest request);

  // 공간 삭제 (ADMIN 권한 검증)
  void deleteSpace(UUID userId, UUID spaceId);

  // 공간 내 유저 권한 변경
  void changeUserRole(UUID adminUserId, UUID spaceId, UUID targetUserId, UserRole role);

  // 전체 공간 조회 (SUPER_ADMIN 권한 검증)
  Page<Space> getAllSpaces(Pageable pageable);
}
