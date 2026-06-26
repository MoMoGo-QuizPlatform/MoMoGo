package com.momogo.core.domain.space.service;

import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.entity.Space;
import java.util.List;
import java.util.UUID;

public interface SpaceService {

  // 공간 개설 (개설자는 ADMIN으로 Role 변경)
  Space createSpace(UUID userId, SpaceCreateRequest request);

  // 비밀번호 기반 공간 가입
  void joinSpace(UUID userId, UUID spaceId, String password);

  // 내가 현재 소속된 공간 정보 조회
  Space getMySpace(UUID userId);

  // 내가 가입하지 않은 전체 공간 목록 조회
  List<Space> getUnjoinedSpaces(UUID userId);

  // 공간 정보 수정 (ADMIN 권한 검증)
  Space updateSpace(UUID userId, UUID spaceId, SpaceUpdateRequest request);

  // 공간 삭제 (ADMIN 권한 검증)
  void deleteSpace(UUID userId, UUID spaceId);
}
