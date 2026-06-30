package com.momogo.api.room;

import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.service.RoomService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;

  @PostMapping("/spaces/{spaceId}/rooms")
  public ResponseEntity<RoomResponse> createRoom(
      @RequestHeader("X-User-Id")UUID userId,
      @PathVariable UUID spaceId,
      @Valid @RequestBody RoomCreateRequest request
  ) {

    RoomResponse response = roomService.createRoom(userId, spaceId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
