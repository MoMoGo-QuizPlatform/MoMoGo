package com.momogo.api.room.consumer;

import com.momogo.core.common.config.KafkaTopics;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import com.momogo.core.domain.room.entity.UserRoomAnswer;
import com.momogo.core.domain.room.event.RoomSubmitEventMessage;
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

  private static final String DEDUP_KEY_PREFIX = "room:submit:processed:";
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

    // 1. Redis 멱등성 키 검증 (중복 처리 방지)
    String dedupKey = DEDUP_KEY_PREFIX + message.eventId();
    if (Boolean.TRUE.equals(redisTemplate.hasKey(dedupKey))) {
      log.warn("[RoomSubmitKafkaConsumer] 이미 처리된 답안 제출 이벤트, 중복 스킵 - eventId: {}", message.eventId());
      return;
    }

    // 2. 추가 SELECT 없이 EntityManager 프록시 참조 생성
    User userProxy = entityManager.getReference(User.class, message.userId());

    // 3. 문제 목록 조회 및 답안 일괄 매핑
    List<UUID> problemIds = message.answers().stream()
        .map(ans -> ans.roomProblemId())
        .toList();

    Map<UUID, RoomProblem> problemsById = roomProblemRepository.findAllById(problemIds).stream()
        .collect(Collectors.toMap(RoomProblem::getId, Function.identity()));

    List<UserRoomAnswer> userRoomAnswers = message.answers().stream()
        .map(ans -> UserRoomAnswer.of(userProxy, problemsById.get(ans.roomProblemId()), ans.userAnswer(), null))
        .toList();

    // 4. DB 일괄 저장
    userRoomAnswerRepository.saveAll(userRoomAnswers);

    // 5. 응시 완료 상태 업데이트
    RoomUserId roomUserId = new RoomUserId(message.roomId(), message.userId());
    roomUserRepository.findById(roomUserId).ifPresent(RoomUser::attend);

    // 6. DB 커밋 확정 후에만 Redis 멱등성 처리 완료 키 갱신
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        redisTemplate.opsForValue().set(dedupKey, "1", DEDUP_TTL);
      }
    });

    log.info("[RoomSubmitKafkaConsumer] DB 저장 완료 - userId: {}, roomId: {}, 문항 수: {}",
        message.userId(), message.roomId(), userRoomAnswers.size());
  }
}
