package com.momogo.core.domain.room.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.room.dto.request.RoomProblemCreatedRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemUpdateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemExamResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUserId;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.exception.RoomProblemErrorCode;
import com.momogo.core.domain.room.mapper.RoomProblemMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomRepository;
import com.momogo.core.domain.room.repository.RoomUserRepository;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomProblemServiceImpl implements RoomProblemService {

  private final RoomRepository roomRepository;

  private final RoomProblemRepository roomProblemRepository;

  private final RoomUserRepository roomUserRepository;

  private final UserRepository userRepository;

  private final RoomProblemMapper roomProblemMapper;

  /**
   * 방 문제 추가 (ADMIN 전용)
   */
  @Override
  @Transactional
  public RoomProblemResponse createRoomProblem(
      UUID userId,
      UUID roomId,
      RoomProblemCreatedRequest request) {

    Room room = getRoom(roomId);

    validateAdmin(userId, room);

    RoomProblem roomProblem = RoomProblem.of(
        room, request.problemOrder(), request.name(),
        request.content(), request.explanation(), request.correctAnswer());

    RoomProblem saved = roomProblemRepository.save(roomProblem);

    return roomProblemMapper.toResponse(saved);
  }

  /**
   * 방 문제 목록 조회 (ADMIN 전용, 시험 시작 여부 무관 항상 조회 가능)
   */
  @Override
  public List<RoomProblemResponse> getRoomProblemsForAdmin(UUID userId, UUID roomId) {

    Room room = getRoom(roomId);

    validateAdmin(userId, room);

    List<RoomProblem> problems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);

    return roomProblemMapper.toResponseList(problems);
  }

  /**
   * 방 문제 목록 조회 (응시자 전용, 시험 시작 전엔 조회 불가, 정답/해설 안 보임)
   */
  @Override
  public List<RoomProblemExamResponse> getRoomProblemsForExam(UUID userId, UUID roomId) {

    Room room = getRoom(roomId);

    // 참가자 검증
    if (!roomUserRepository.existsById(new RoomUserId(roomId, userId))) {
      throw new BusinessException(RoomProblemErrorCode.ROOM_ACCESS_DENIED);
    }

    // 시험 시작 검증
    if (room.getTestStartAt().isAfter(OffsetDateTime.now())) {
      throw new BusinessException(RoomProblemErrorCode.ROOM_NOT_STARTED);
    }

    List<RoomProblem> problems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);

    return roomProblemMapper.toExamResponseList(problems);
  }

  /**
   * 방 문제 수정 (ADMIN 전용)
   */
  @Override
  @Transactional
  public RoomProblemResponse updateRoomProblem(
      UUID userId,
      UUID roomId,
      UUID roomProblemId,
      RoomProblemUpdateRequest request) {

    Room room = getRoom(roomId);

    validateAdmin(userId, room);

    RoomProblem roomProblem = getRoomProblem(roomId, roomProblemId);

    roomProblem.update(null, request.name(), request.content(), request.explanation(), request.correctAnswer());

    return roomProblemMapper.toResponse(roomProblem);
  }

  /**
   * 방 문제 삭제 (ADMIN 전용) - 삭제 후 순번 재정렬
   */
  @Override
  @Transactional
  public void deleteRoomProblem(UUID userId, UUID roomId, UUID roomProblemId) {

    Room room = getRoom(roomId);

    validateAdmin(userId, room);

    RoomProblem target = getRoomProblem(roomId, roomProblemId);

    // 순번 재정렬
    roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId)
        .stream()
        .filter(p -> p.getProblemOrder() > target.getProblemOrder())
        .forEach(p -> p.update(p.getProblemOrder() - 1, null, null, null, null));

    roomProblemRepository.delete(target);
  }

  private Room getRoom(UUID roomId) {

    return roomRepository.findById(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  private RoomProblem getRoomProblem(UUID roomId, UUID roomProblemId) {

    RoomProblem roomProblem = roomProblemRepository.findById(roomProblemId)
        .orElseThrow(() -> new BusinessException(RoomProblemErrorCode.NOT_FOUND));

    if (!roomProblem.getRoom().getId().equals(roomId)) {
      throw new BusinessException(RoomProblemErrorCode.NOT_FOUND);
    }

    return roomProblem;
  }

  private void validateAdmin(UUID userId, Room room) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(room.getSpace().getId())) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }
  }
}
