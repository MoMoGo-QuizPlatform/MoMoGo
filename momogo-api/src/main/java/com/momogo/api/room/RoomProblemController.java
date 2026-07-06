package com.momogo.api.room;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.core.domain.room.dto.request.RoomProblemCreatedRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemUpdateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemExamResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.service.RoomProblemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/problems")
public class RoomProblemController {

  private final RoomProblemService roomProblemService;

  /**
   * 방 문제 추가 (ADMIN 전용)
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RoomProblemResponse> createRoomProblem(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID roomId,
      @Valid @RequestBody RoomProblemCreatedRequest request) {

    RoomProblemResponse response = roomProblemService.createRoomProblem(userDetails.getUserResponse().id(), roomId, request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  /**
   * 방 문제 목록 조회 (ADMIN 전용, 시험 시작 여부 무관 항상 조회 가능)
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<RoomProblemResponse>> getRoomProblemsForAdmin(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID roomId) {

    List<RoomProblemResponse> response = roomProblemService.getRoomProblemsForAdmin(
        userDetails.getUserResponse().id(), roomId);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  /**
   * 방 문제 목록 조회 (응시자 전용, 시험 시작 전엔 조회 불가, 정답/해설 안 보임)
   */
  @GetMapping("/exam")
  public ResponseEntity<List<RoomProblemExamResponse>> getRoomProblemsForExam(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID roomId) {

    List<RoomProblemExamResponse> response = roomProblemService.getRoomProblemsForExam(
        userDetails.getUserResponse().id(), roomId);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  /**
   * 방 문제 수정 (ADMIN 전용)
   */
  @PatchMapping("/{roomProblemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<RoomProblemResponse> updateRoomProblem(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID roomId,
      @PathVariable UUID roomProblemId,
      @Valid @RequestBody RoomProblemUpdateRequest request) {

    RoomProblemResponse response = roomProblemService.updateRoomProblem(userDetails.getUserResponse().id(), roomId, roomProblemId, request);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  /**
   * 방 문제 삭제 (ADMIN 전용) - 삭제 후 순번 재정렬
   */
  @DeleteMapping("/{roomProblemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteRoomProblem(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID roomId,
      @PathVariable UUID roomProblemId) {

    roomProblemService.deleteRoomProblem(userDetails.getUserResponse().id(), roomId, roomProblemId);

    return ResponseEntity
        .noContent()
        .build();
  }
}
