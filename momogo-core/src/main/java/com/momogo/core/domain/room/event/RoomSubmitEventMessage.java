package com.momogo.core.domain.room.event;

import com.momogo.core.domain.room.dto.request.ProblemAnswerRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record RoomSubmitEventMessage(
    UUID eventId,        // 카프카 재전달 시 중복 처리 방지용 식별자 (Redis Dedup 키)
    UUID userId,
    UUID roomId,
    List<ProblemAnswerRequest> answers,
    OffsetDateTime submittedAt
) {

  public RoomSubmitEventMessage {
    Objects.requireNonNull(eventId, "eventId는 null일 수 없습니다");
    Objects.requireNonNull(userId, "userId는 null일 수 없습니다");
    Objects.requireNonNull(roomId, "roomId는 null일 수 없습니다");
    Objects.requireNonNull(answers, "answers는 null일 수 없습니다");
    Objects.requireNonNull(submittedAt, "submittedAt은 null일 수 없습니다");
    answers = List.copyOf(answers);
  }

  public static RoomSubmitEventMessage of(UUID userId, UUID roomId, List<ProblemAnswerRequest> answers) {
    return new RoomSubmitEventMessage(UUID.randomUUID(), userId, roomId, answers, OffsetDateTime.now());
  }
}
