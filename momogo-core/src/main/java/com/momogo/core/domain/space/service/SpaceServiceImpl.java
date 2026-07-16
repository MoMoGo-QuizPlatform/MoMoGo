package com.momogo.core.domain.space.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.security.PasswordEncryptor;
import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.dto.response.SpaceCursorPaginationResponse;
import com.momogo.core.domain.space.dto.response.SpaceResponse;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.space.mapper.SpaceMapper;
import com.momogo.core.domain.space.event.SpaceUserJoinedEvent;
import com.momogo.core.domain.space.event.SpaceUserRoleChangedEvent;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final ApplicationEventPublisher eventPublisher;

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
     user.joinSpace(savedSpace, UserRole.ADMIN);

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
    user.joinSpace(space, UserRole.USER);

    // 공간 ADMIN에게 알림을 보내기 위한 이벤트 발행
    eventPublisher.publishEvent(new SpaceUserJoinedEvent(spaceId, userId));
  }

  @Override
  public Optional<SpaceResponse> getMySpace(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    // 트랜잭션(세션)이 열려 있는 동안 lazy 프록시인 space를 매핑까지 끝내야 한다.
    // open-in-view: false 설정 때문에, 매핑을 컨트롤러로 미루면 세션이 닫힌 뒤 접근하게 되어
    // LazyInitializationException이 발생한다.
    return Optional.ofNullable(user.getSpace()).map(spaceMapper::toResponse);
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
    if (hasNext && !content.isEmpty()) {
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

    // 벌크 쿼리로 공간 소속 유저 탈퇴 처리
    userRepository.bulkLeaveSpace(spaceId, UserRole.USER);

    spaceRepository.delete(space);
  }

  @Override
  @Transactional
  public void changeUserRole(UUID adminUserId, UUID spaceId, UUID targetUserId, UserRole role) {

    // 요청 보낸 유저가 해당 공간의 ADMIN 권한을 가졌는지 검증
    validateAndGetSpaceAdmin(adminUserId, spaceId);

    // 본인의 권한을 스스로 변경하는 행위 차단
    if (adminUserId.equals(targetUserId)) {
      throw new BusinessException(SpaceErrorCode.SELF_ROLE_CHANGE_BLOCKED);
    }

    // 권한 변경 대상 유저 조회
    User targetUser = userRepository.findById(targetUserId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    // 대상 유저가 나와 같은 공간에 소속되어 있는지 체크
    if (targetUser.getSpace() == null || !targetUser.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER);
    }

    // 권한 변경
    targetUser.changeRole(role);

    // 권한이 변경된 유저에게 알림을 보내기 위한 이벤트 발행
    eventPublisher.publishEvent(new SpaceUserRoleChangedEvent(targetUserId, spaceId, role));
  }

  @Override
  public Page<Space> getAllSpaces(Pageable pageable) {
    return spaceRepository.findAll(pageable);
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
