package com.momogo.api.notification.redis;

import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import java.util.UUID;

/*
 * SSE 알림을 인스턴스 간에 중계하기 위해 Redis 채널에 실어보내는 메시지.
 * userId로 "누구에게 보낼지"를, notification으로 "무엇을 보낼지"를 담는다.
 */
public record NotificationSseMessage(
    UUID userId,
    NotificationResponse notification
) {
}
