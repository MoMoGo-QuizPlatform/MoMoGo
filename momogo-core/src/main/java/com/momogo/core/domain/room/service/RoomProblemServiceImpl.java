package com.momogo.core.domain.room.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.repository.ProblemCategoryRepository;
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
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomProblemServiceImpl implements RoomProblemService {

  // GET한 값이 내 토큰과 같을 때만 DEL - TTL 만료로 락이 다른 요청에 넘어간 경우 잘못 삭제하는 것 방지
  private static final RedisScript<Long> UNLOCK_SCRIPT = RedisScript.of("""
      if redis.call('get', KEYS[1]) == ARGV[1] then
        return redis.call('del', KEYS[1])
      else
        return 0
      end
      """, Long.class);

  private final RoomRepository roomRepository;

  private final RedisTemplate<String, Object> redisTemplate;

  private final RoomProblemRepository roomProblemRepository;

  private final UserRepository userRepository;

  private final RoomProblemMapper roomProblemMapper;

  private final ProblemCategoryRepository categoryRepository;

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

    ProblemCategory category = getCategory(request.categoryId());

    RoomProblem roomProblem = RoomProblem.of(
        room, category, request.problemOrder(), request.name(),
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

    ProblemCategory category =
        request.categoryId() != null ? getCategory(request.categoryId()) : null;

    roomProblem.update(category, null, request.name(), request.content(), request.explanation(),
        request.correctAnswer());

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
      UUID idempotencyKey,
      RoomProblemAiCreateRequest request) {

    // 같은 idempotencyKey로 들어온 재시도(더블클릭/네트워크 재전송)를 차단
    // 인스턴스가 여러 대라도 Redis가 공용 상태를 갖고 있어 판단 가능
    String lockKey = "idem:room-problem-ai:" + idempotencyKey;
    // 락 값을 요청 식별 토큰으로 - TTL 만료 후 다른 요청이 같은 키로 락을 새로 잡았을 때 구분하기 위함
    String ownerToken = UUID.randomUUID().toString();

    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, ownerToken, Duration.ofMinutes(10));

    if (Boolean.FALSE.equals(acquired)) {
      throw new BusinessException(RoomProblemErrorCode.DUPLICATE_AI_REQUEST);
    }

    try {
      Room room = roomRepository.findByIdWithSpace(roomId)
          .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

      validateAdmin(userId, room);

      ProblemCategory category = getCategory(request.categoryId());

      List<GeneratedProblemData> generated = problemGenerationService.generateProblems(
          request.referenceText(),
          request.questionCount()
      );

      return roomProblemPersister.saveGeneratedProblems(roomId, category, generated);
    } finally {
      // 성공/실패/Error 무관 항상 해제. 무조건 delete 대신 내 토큰일 때만 삭제 (TTL 초과로 락이 넘어갔으면 남의 락을 지우지 않음)
      redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), ownerToken);
    }
  }


  private Room getRoom(UUID roomId) {

    return roomRepository.findById(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  private ProblemCategory getCategory(UUID categoryId) {

    return categoryRepository.findById(categoryId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.CATEGORY_NOT_FOUND));
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

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null
        || !user.getSpace().getId().equals(room.getSpace().getId())) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }
  }
}
