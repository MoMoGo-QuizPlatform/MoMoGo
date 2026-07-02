package com.momogo.api.room;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.service.RoomService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  /**
   * 평가 시험 개설
   * @param userDetails 유저 아이디
   * @param spaceId 공간 아이디
   * @param request 평가 시험 개설 요청 DTO
   * @return 개설된 평가 시험 정보
   */
  @PostMapping("/spaces/{spaceId}/rooms")
  public ResponseEntity<RoomResponse> createRoom(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID spaceId,
      @Valid @RequestBody RoomCreateRequest request
  ) {

    RoomResponse response = roomService.createRoom(userDetails.getUserResponse().id(), spaceId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
