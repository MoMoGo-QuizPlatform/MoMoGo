package com.momogo.core.domain.room.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.service.ProblemGenerationService;
import com.momogo.core.domain.room.dto.request.RoomProblemAiCreateRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemCreatedRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemUpdateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.exception.RoomProblemErrorCode;
import com.momogo.core.domain.room.mapper.RoomProblemMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomRepository;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomProblemServiceImpl implements RoomProblemService {

  private final RoomRepository roomRepository;

  private final RoomProblemRepository roomProblemRepository;

  private final UserRepository userRepository;

  private final RoomProblemMapper roomProblemMapper;

  private final ProblemGenerationService problemGenerationService;

  private final RoomProblemPersister roomProblemPersister;

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
    roomProblemRepository.decreaseOrderAfter(roomId, target.getProblemOrder());

    roomProblemRepository.delete(target);
  }

  /**
   * 방 문제 AI 자동 생성 (ADMIN 전용)
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED) // 해당 메서드 자체는 트랜잭션 없이 실행
  public List<RoomProblemResponse> createRoomProblemsByAi(
      UUID userId,
      UUID roomId,
      RoomProblemAiCreateRequest request) {

    Room room = roomRepository.findByIdWithSpace(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

    validateAdmin(userId, room);

    List<GeneratedProblemData> generated = problemGenerationService.generateProblems(
        request.referenceText(),
        request.questionCount()
    );

    return roomProblemPersister.saveGeneratedProblems(room, generated);
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

    User user = userRepository.findByIdWithSpace(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(room.getSpace().getId())) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }
  }
}
