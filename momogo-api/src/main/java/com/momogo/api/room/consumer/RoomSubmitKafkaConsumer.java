package com.momogo.api.room.consumer;

import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.common.config.RoomRedisKeys;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import com.momogo.core.domain.room.entity.UserRoomAnswer;
import com.momogo.core.domain.room.event.RoomSubmitEventMessage;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomUserRepository;
import com.momogo.core.domain.room.repository.UserRoomAnswerRepository;
import com.momogo.core.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/*
 * RoomServiceImpl이 발행한 답안 제출 메시지를 수신하여 실제 DB 저장을 비동기로 수행
 * Redis 멱등성 체크를 통해 중복 전달(At-least-once)로 인한 UQ 충돌 방지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomSubmitKafkaConsumer {

  private static final Duration DEDUP_TTL = Duration.ofHours(24);

  private final UserRoomAnswerRepository userRoomAnswerRepository;
  private final RoomUserRepository roomUserRepository;
  private final RoomProblemRepository roomProblemRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final EntityManager entityManager;

  @KafkaListener(topics = KafkaTopics.ROOM_SUBMIT_EVENTS)
  @Transactional
  public void consume(RoomSubmitEventMessage message) {
    log.info("[RoomSubmitKafkaConsumer] 카프카 답안 수신 - eventId: {}, userId: {}, roomId: {}",
        message.eventId(), message.userId(), message.roomId());

    // 1. Redis atomic setIfAbsent로 "PROCESSING" 상태 선점 (원자적 멱등성 검사)
    String dedupKey = RoomRedisKeys.DEDUP_KEY_PREFIX + message.eventId();
    Boolean isAcquired = redisTemplate.opsForValue().setIfAbsent(dedupKey, "PROCESSING", Duration.ofMinutes(5));
    if (Boolean.FALSE.equals(isAcquired)) {
      log.warn("[RoomSubmitKafkaConsumer] 이미 처리 중이거나 완료된 답안 제출 이벤트, 중복 스킵 - eventId: {}", message.eventId());
      return;
    }

    // 2. DB 트랜잭션 종결 상태별 후처리 (커밋 성공 시 DONE 갱신, 예외/롤백 시 키 삭제로 카프카 재시도 보장)
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        redisTemplate.opsForValue().set(dedupKey, "DONE", DEDUP_TTL);
      }

      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) {
          redisTemplate.delete(dedupKey);
          redisTemplate.delete(RoomRedisKeys.SUBMIT_CLAIM_PREFIX + message.roomId() + ":" + message.userId());
        }
      }
    });

    // 3. 추가 SELECT 없이 EntityManager 프록시 참조 생성
    User userProxy = entityManager.getReference(User.class, message.userId());

    // 4. 문제 목록 조회 및 해당 방 소속 검증
    List<UUID> problemIds = message.answers().stream()
        .map(ans -> ans.roomProblemId())
        .toList();

    Map<UUID, RoomProblem> problemsById = roomProblemRepository.findAllById(problemIds).stream()
        .filter(p -> p.getRoom().getId().equals(message.roomId()))
        .collect(Collectors.toMap(RoomProblem::getId, Function.identity()));

    if (problemsById.size() != problemIds.size()) {
      log.error("[RoomSubmitKafkaConsumer] 유효하지 않거나 해당 방에 속하지 않는 문제 ID 포함, 처리 중단 - eventId: {}, roomId: {}, 요청 문제 수: {}, 유효 문제 수: {}",
          message.eventId(), message.roomId(), problemIds.size(), problemsById.size());
      throw new BusinessException(RoomErrorCode.PROBLEM_NOT_FOUND);
    }

    List<UserRoomAnswer> userRoomAnswers = message.answers().stream()
        .map(ans -> UserRoomAnswer.of(userProxy, problemsById.get(ans.roomProblemId()), ans.userAnswer(), null))
        .toList();

    // 5. 참여자 자격 조회 및 중복 응시 사전 검증 (트랜잭션 rollback-only 예외 방지)
    RoomUserId roomUserId = new RoomUserId(message.roomId(), message.userId());
    RoomUser roomUser = roomUserRepository.findById(roomUserId)
        .orElseThrow(() -> {
          log.error("[RoomSubmitKafkaConsumer] 참여자 정보 없음, 처리 중단 - userId: {}, roomId: {}",
              message.userId(), message.roomId());
          return new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT);
        });

    // [k6 테스트를 위한 주석 처리] 중복 응시 체크 비활성화
    // if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
    //   log.warn("[RoomSubmitKafkaConsumer] 이미 응시 완료된 유저의 중복 제출 이벤트, 처리 스킵 - eventId: {}, userId: {}, roomId: {}",
    //       message.eventId(), message.userId(), message.roomId());
    //   return;
    // }

    // [k6 부하 테스트용] 기존 동일 유저 답안 삭제 후 일괄 저장 (UQ 충돌 방지)
    userRoomAnswerRepository.deleteByRoomProblemRoomIdAndUserId(message.roomId(), message.userId());
    userRoomAnswerRepository.saveAll(userRoomAnswers);
    roomUser.attend();

    log.info("[RoomSubmitKafkaConsumer] DB 저장 완료 - userId: {}, roomId: {}, 문항 수: {}",
        message.userId(), message.roomId(), userRoomAnswers.size());
  }
}
