package com.momogo.api.room;

import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoomController {

  private final RoomService roomService;

  /**
   * 평가 시험 개설
   * @param userId 유저 아이디
   * @param spaceId 공간 아이디
   * @param request 평가 시험 개설 요청 DTO
   * @return 개설된 평가 시험 정보
   */
  @PostMapping("/spaces/{spaceId}/rooms")
  public ResponseEntity<RoomResponse> createRoom(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @PathVariable UUID spaceId,
      @Valid @RequestBody RoomCreateRequest request
  ) {

    RoomResponse response = roomService.createRoom(userId, spaceId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 평가 시험 상세 조회
   * @param userId 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 평가 시험 상세 정보
   */
  @GetMapping("/rooms/{roomId}")
  public ResponseEntity<RoomResponse> getRoomDetails(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @PathVariable UUID roomId
  ) {
    RoomResponse response = roomService.getRoomDetails(userId, roomId);
    return ResponseEntity.ok(response);
  }

  /**
   * 평가 시험 문제지 조회
   * @param userId 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 평가 시험 문제지
   */
  @GetMapping("/rooms/{roomId}/problems")
  public ResponseEntity<List<RoomProblemResponse>> getRoomProblems(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @PathVariable UUID roomId
  ) {
    List<RoomProblemResponse> response = roomService.getRoomProblems(userId, roomId);
    return ResponseEntity.ok(response);
  }

  /**
   * 평가 시험 답안 제출
   * @param userId 유저 아이디
   * @param roomId 평가 시험 아이디
   * @param request 답안 제출 요청 DTO
   * @return 답안 제출 성공 여부
   */
  @PostMapping("/rooms/{roomId}/submit")
  public ResponseEntity<Void> submitRoomAnswer(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @PathVariable UUID roomId,
      @Valid @RequestBody RoomAnswerSubmitRequest request
  ) {
    roomService.submitRoomAnswer(userId, roomId, request);
    return ResponseEntity.ok().build();
  }

  /**
   * 평가 시험 채점 최종 마감
   * @param adminUserId 관리자 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 채점 최종 마감 성공 여부
   */
  @PostMapping("/rooms/{roomId}/finalize-grade")
  public ResponseEntity<Void> finalizeGrade(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID adminUserId,
      @PathVariable UUID roomId
  ) {
    roomService.finalizeGrade(adminUserId, roomId);
    return ResponseEntity.ok().build();
  }
}
