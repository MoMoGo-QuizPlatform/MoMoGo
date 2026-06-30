package com.momogo.core.domain.room.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.event.RoomCreatedEvent;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.mapper.RoomMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomRepository;
import com.momogo.core.domain.room.repository.RoomUserRepository;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService{

  private final RoomRepository roomRepository;
  private final RoomProblemRepository roomProblemRepository;
  private final RoomUserRepository roomUserRepository;
  private final UserRepository userRepository;
  private final SpaceRepository spaceRepository;
  private final RoomMapper roomMapper;
  private final ApplicationEventPublisher eventPublisher;

  // 검증된 타겟 객체들을 묶기 위한 private record
  private record ValidatedRoomTarget(Space space, List<User> targetUsers) {}

  @Override
  @Transactional
  public RoomResponse createRoom(UUID userId, UUID spaceId, RoomCreateRequest request) {

    log.info("[RoomService] 평가 시험방 생성 시작 - userId: {}, spaceId: {}, roomName: {}", userId, spaceId, request.name());

    // 1. 유효성 검증 및 데이터 조회
    ValidatedRoomTarget targets = validateAndGetTargets(userId, spaceId, request);

    // 2. Room 생성 및 저장
    Room savedRoom = roomRepository.save(Room.of(
        targets.space(),
        request.name(),
        request.description(),
        request.testStartAt(),
        request.testEndAt()
    ));

    // 3. RoomUser 매핑 관계 생성 및 저장
    List<RoomUser> roomUsers = targets.targetUsers().stream()
        .map(targetUser -> RoomUser.of(savedRoom, targetUser))
        .toList();
    roomUserRepository.saveAll(roomUsers);

    // 4. RoomProblem 생성 및 저장
    List<RoomProblem> roomProblems = request.problems().stream()
        .map(prob -> RoomProblem.of(
            savedRoom,
            prob.problemOrder(),
            prob.name(),
            prob.content(),
            prob.explanation(),
            prob.correctAnswer()))
        .toList();
    roomProblemRepository.saveAll(roomProblems);

    log.info("[RoomService] 평가 시험방 생성 성공 - roomId: {}", savedRoom.getId());

    // 알림용 Spring Event 발행
    eventPublisher.publishEvent(new RoomCreatedEvent(
        savedRoom.getId(),
        targets.space.getId(),
        savedRoom.getName(),
        request.userIds()
    ));

    return roomMapper.toResponse(savedRoom);
  }

  // 시험방 생성을 위한 유효성 검증 및 도메인 엔티티 일괄 조회 헬퍼 메서드
  private ValidatedRoomTarget validateAndGetTargets(UUID userId, UUID spaceId, RoomCreateRequest request) {

    // 개설 유저의 공간 관리자(ADMIN) 권한 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }

    // 공간(Space) 존재 여부 검증 및 획득
    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    // 응시 대상 유저 검증 및 획득
    List<User> targetUsers = userRepository.findAllById(request.userIds());
    if (targetUsers.size() != request.userIds().size()) {
      log.warn("[RoomService] 응시 대상 유저 중 일부가 존재하지 않습니다. 요청: {}, 조회: {}",
          request.userIds().size(), targetUsers.size());
      throw new BusinessException(RoomErrorCode.ROOM_USER_NOT_FOUND);
    }

    // 공간 경계 검증 (모든 응시 유저가 이 공간 소속인지 확인)
    boolean hasExternalUser = targetUsers.stream()
        .anyMatch(u -> u.getSpace() == null || !u.getSpace().getId().equals(spaceId));

    if (hasExternalUser) {
      log.warn("[RoomService] 해당 공간 소속이 아닌 외부 유저가 응시 대상에 포함되어 있습니다. spaceId: {}", spaceId);
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER);
    }

    return new ValidatedRoomTarget(space, targetUsers);
  }

}
