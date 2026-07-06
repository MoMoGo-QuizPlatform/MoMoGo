package com.momogo.core.domain.room.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.room.dto.request.ProblemAnswerRequest;
import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import com.momogo.core.domain.room.entity.UserRoomAnswer;
import com.momogo.core.domain.room.event.RoomCreatedEvent;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.mapper.RoomMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomRepository;
import com.momogo.core.domain.room.repository.RoomUserRepository;
import com.momogo.core.domain.room.repository.UserRoomAnswerRepository;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final UserRoomAnswerRepository userRoomAnswerRepository;
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

  @Override
  public RoomResponse getRoomDetails(UUID userId, UUID roomId) {
    log.info("[RoomService] 시험방 상세 조회 - userId: {}, roomId: {}", userId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 권한 검증 - 요청 유저가 해당 방의 소속 공간 멤버가 맞는지 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getSpace() == null || !user.getSpace().getId().equals(room.getSpace().getId())) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER);
    }

    return  roomMapper.toResponse(room);
  }

  @Override
  public List<RoomProblemResponse> getRoomProblems(UUID userId, UUID roomId) {
    log.info("[RoomService] 시험 문제지 조회 - userId: {}, roomId: {}", userId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 시간 검증 - 현재 시각이 시험 시작 시각 이전인지 확인
    if (OffsetDateTime.now().isBefore(room.getTestStartAt())) {
      log.warn("[RoomService] 시험 시작 전 문제 조회 차단 - roomId: {}, testStartAt: {}", roomId, room.getTestStartAt());
      throw new BusinessException(RoomErrorCode.INVALID_ACCESS_BEFORE_START);
    }

    // 응시 대상 유저 자격 검증 (RoomUser 매핑 테이블 존재 확인)
    RoomUserId roomUserId = new RoomUserId(roomId, userId);
    if (!roomUserRepository.existsById(roomUserId)) {
      throw new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT);
    }

    // 문제 목록 조회 및 정렬 반환
    List<RoomProblem> roomProblems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);

    return roomMapper.toProblemResponseList(roomProblems);
  }

  @Override
  @Transactional
  public void submitRoomAnswer(UUID userId, UUID roomId, RoomAnswerSubmitRequest request) {
    log.info("[RoomService] 시험 답안 제출 시작 - userId: {}, roomId: {}", userId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 시간 검증 - 시험 시작 전 제출 시도 차단
    OffsetDateTime now = OffsetDateTime.now();
    if (now.isBefore(room.getTestStartAt())) {
      throw new BusinessException(RoomErrorCode.INVALID_ACCESS_BEFORE_START);
    }

    // 상태 검증 - 이미 최종적으로 끝난 시험이면 추가 제출 불가
    if (Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 응시 대상 유저  자격 검증
    RoomUserId roomUserId = new RoomUserId(roomId, userId);
    RoomUser roomUser = roomUserRepository.findById(roomUserId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER));

    // 요청 유저 획득
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    List<UUID> problemIds = request.answers().stream().map(ProblemAnswerRequest::roomProblemId).toList();
    Map<UUID, RoomProblem> problemsById = roomProblemRepository.findAllById(problemIds).stream()
        .collect(Collectors.toMap(RoomProblem::getId, Function.identity()));

    if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 각 문제 답안 일괄 매핑, 저장
    List<UserRoomAnswer> userRoomAnswers = request.answers().stream()
        .map(ans -> {
          RoomProblem problem = problemsById.get(ans.roomProblemId());
          if (problem == null || !problem.getRoom().getId().equals(roomId)) {
            throw new BusinessException(RoomErrorCode.PROBLEM_NOT_FOUND);
          }
          return UserRoomAnswer.of(user, problem, ans.userAnswer());
        })
        .toList();
    userRoomAnswerRepository.saveAll(userRoomAnswers);

    // 응시 완료 상태 업데이트
    roomUser.attend();

    log.info("[RoomService] 시험 답안 제출 완료 - userId: {}, roomId: {}, 제출 문항 수: {}",
        userId, roomId, userRoomAnswers.size());
  }


  // 시험방 생성을 위한 유효성 검증 및 도메인 엔티티 일괄 조회 헬퍼 메서드
  private ValidatedRoomTarget validateAndGetTargets(UUID userId, UUID spaceId, RoomCreateRequest request) {

    // 개설 유저의 공간 관리자(ADMIN) 권한 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));


    // 공간(Space) 존재 여부 검증 및 획득
    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));


    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }

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

  private Room findRoomOrThrow(UUID roomId) {
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }
}
