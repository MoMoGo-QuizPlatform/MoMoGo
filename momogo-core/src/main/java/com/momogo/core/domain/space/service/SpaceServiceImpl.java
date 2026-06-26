package com.momogo.core.domain.space.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.dto.response.SpaceCursorPaginationResponse;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.space.mapper.SpaceMapper;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import com.momogo.core.common.security.PasswordEncryptor;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceServiceImpl implements SpaceService {

  private final SpaceRepository spaceRepository;
  private final UserRepository userRepository;
  private final SpaceMapper spaceMapper;
  private final PasswordEncryptor passwordEncryptor;

  @Override
  @Transactional
  public Space createSpace(UUID userId, SpaceCreateRequest request) {

    // 유저 검증, 공간 가입 상태 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getSpace() != null) {
      throw new BusinessException(SpaceErrorCode.ALREADY_IN_SPACE);
    }

    // 공간 개설 및 저장 (비밀번호 단방향 해시 암호화 적용)
    Space space = Space.builder()
        .id(UUID.randomUUID())
        .name(request.name())
        .description(request.description())
        .spaceImageUrl(request.spaceImageUrl())
        .spacePassword(passwordEncryptor.encrypt(request.spacePassword()))
        .build();
    Space savedSpace = spaceRepository.save(space);

    // 개설한 유저의 공간 정보 및 역할을 ADMIN으로 업데이트
    // user.joinSpace(savedSpace, UserRole.ADMIN);

    return savedSpace;
  }

  @Override
  @Transactional
  public void joinSpace(UUID userId, UUID spaceId, String password) {

    // 유저 및 공간 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getSpace() != null) {
      throw new BusinessException(SpaceErrorCode.ALREADY_IN_SPACE);
    }

    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    // 비밀번호 체크 (단방향 해시 매칭 검증)
    if (!passwordEncryptor.matches(password, space.getSpacePassword())) {
      throw new BusinessException(SpaceErrorCode.WRONG_SPACE_PASSWORD);
    }

    // 공간 가입 처리
    //user.joinSpace(space, UserRole.USER);
  }

  @Override
  public Optional<Space> getMySpace(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    return Optional.ofNullable(user.getSpace());
  }

  @Override
  public SpaceCursorPaginationResponse<Space> getUnjoinedSpacesByCursor(
      UUID userId, UUID lastSpaceId, OffsetDateTime lastCreatedAt, int size) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    UUID userSpaceId = user.getSpace() != null ? user.getSpace().getId() : null;

    // Limit + 1 개수만큼 QueryDSL 조회
    List<Space> spaces = spaceRepository.findUnjoinedSpacesByCursor(userSpaceId, lastSpaceId, lastCreatedAt, size);

    // 다음 페이지 존재 여부 판단
    boolean hasNext = spaces.size() > size;

    // HasNext가 true인 경우, 마지막에 가져온 가짜 초과 데이터 1개 제거
    List<Space> content = hasNext ? spaces.subList(0, size) : spaces;

    // 다음 조회를 위한 커서 문자열 생성
    String nextCursor = null;
    if (!content.isEmpty()) {
      Space lastSpace = content.get(content.size() - 1);
      nextCursor = lastSpace.getId().toString() + "_" + lastSpace.getCreatedAt().toString();
    }

    return new SpaceCursorPaginationResponse<>(content, hasNext, nextCursor);
  }

  @Override
  @Transactional
  public Space updateSpace(UUID userId, UUID spaceId, SpaceUpdateRequest request) {

    // 유저 및 ADMIN 권한 검증
    validateAndGetSpaceAdmin(userId, spaceId);

    // 공간 조회 및 수정
    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    spaceMapper.updateFromDto(request, space); // MapStruct 더티 체킹

    if (request.spacePassword() != null && !request.spacePassword().isBlank()) {
      space.updateSpacePassword(passwordEncryptor.encrypt(request.spacePassword()));
    }

    return space;
  }

  @Override
  @Transactional
  public void deleteSpace(UUID userId, UUID spaceId) {

    // 유저 및 ADMIN 권한 검증
    validateAndGetSpaceAdmin(userId, spaceId);

    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    // 소속되어 있는 모든 유저 탈퇴 처리
//    List<User> members = userRepository.findAllBySpaceId(spaceId);
//    for (User member : members) {
//      member.leaveSpace();
//    }

    spaceRepository.delete(space);
  }
 
  // 공통 헬퍼 메소드: 사용자 조회 및 공간 관리자 권한 검증
  private User validateAndGetSpaceAdmin(UUID userId, UUID spaceId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));
 
    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }
    return user;
  }
}
