package com.momogo.realtime.websocket.controller;

import com.momogo.realtime.websocket.dto.request.RealtimeMessageRequest;
import com.momogo.realtime.websocket.dto.response.RealtimeMessageResponse;
import com.momogo.realtime.websocket.redis.RedisMessagePublisher;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomRealtimeController {

  private final RedisMessagePublisher redisMessagePublisher;

  @MessageMapping("/room/{roomId}/send")
  public void sendMessage(
      @DestinationVariable("roomId")UUID roomId,
      Principal principal,
      @Payload @Valid RealtimeMessageRequest request
  ) {

    UUID authenticatedUserId = UUID.fromString(principal.getName());

    log.info("[RoomRealtimeController] 시험방({}) 상태 변경 수신 - authenticatedUserId: {}, status: {}",
        roomId, authenticatedUserId, request.status());

    RealtimeMessageResponse response = RealtimeMessageResponse.of(roomId, authenticatedUserId, request);
    redisMessagePublisher.publish(response);
  }
}
