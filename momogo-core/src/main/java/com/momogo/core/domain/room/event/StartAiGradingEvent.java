package com.momogo.core.domain.room.event;

import java.util.UUID;

/**
 * AI 채점 비동기 시작 이벤트
 * @param roomId 시험방 아이디
 */
public record StartAiGradingEvent(
    UUID roomId
) {

}
