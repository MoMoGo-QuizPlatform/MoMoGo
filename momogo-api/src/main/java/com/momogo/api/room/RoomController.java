package com.momogo.api.room;

import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.room.dto.request.ManualGradeRequest;
import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemDraftAiRequest;
import com.momogo.core.domain.room.dto.response.RoomGradingResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.dto.response.RoomReportResponse;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.service.RoomService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
   * 공간 내 평가 시험방 목록 조회 (최근 생성 순)
   * @param userId 유저 아이디
   * @param spaceId 공간 아이디
   * @return 평가 시험방 목록
   */
  @GetMapping("/spaces/{spaceId}/rooms")
  public ResponseEntity<List<RoomResponse>> getRoomList(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @PathVariable UUID spaceId
  ) {
    List<RoomResponse> response = roomService.getRoomList(userId, spaceId);
    return ResponseEntity.ok(response);
  }

  /**
   * 시험방 생성 마법사(2단계) 미리보기용 AI 문제 생성 (ADMIN 전용)
   * 방이 아직 만들어지기 전 단계이므로 DB에는 저장하지 않고 생성 결과만 반환한다.
   * @param spaceId 공간 아이디
   * @param request 참고자료/문항 수 요청 DTO
   * @return 생성된 문제 초안 목록 (미저장)
   */
  @PostMapping("/spaces/{spaceId}/rooms/ai-draft-problems")
  public ResponseEntity<List<GeneratedProblemData>> generateDraftProblems(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @PathVariable UUID spaceId,
      @Valid @RequestBody RoomProblemDraftAiRequest request
  ) {
    return ResponseEntity.ok(roomService.generateDraftProblems(userId, spaceId, request));
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
   * 채점 검토 화면 조회 (응시자별 문제별 제출 답안/모범 정답/현재 정오 판정)
   * @param adminUserId 관리자 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 채점 검토 데이터
   */
  @GetMapping("/rooms/{roomId}/grading")
  public ResponseEntity<RoomGradingResponse> getRoomGrading(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID adminUserId,
      @PathVariable UUID roomId
  ) {
    return ResponseEntity.ok(roomService.getRoomGrading(adminUserId, roomId));
  }

  /**
   * 답안 한 건 수동 채점(정오 판정 오버라이드)
   * @param adminUserId 관리자 유저 아이디
   * @param roomId 평가 시험 아이디
   * @param answerId 답안 아이디
   * @param request 수동 채점 요청 DTO
   * @return 성공 여부
   */
  @PatchMapping("/rooms/{roomId}/grading/{answerId}")
  public ResponseEntity<Void> manualGradeAnswer(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID adminUserId,
      @PathVariable UUID roomId,
      @PathVariable UUID answerId,
      @Valid @RequestBody ManualGradeRequest request
  ) {
    roomService.manualGradeAnswer(adminUserId, roomId, answerId, request);
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

  /**
   * 평가 시험 리포트 조회
   * @param adminUserId 관리자 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 리포트 정보
   */
  @GetMapping("/rooms/{roomId}/report")
  public ResponseEntity<RoomReportResponse> getRoomReport(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID adminUserId,
      @PathVariable UUID roomId
  ) {
    RoomReportResponse response = roomService.getRoomReport(adminUserId, roomId);
    return ResponseEntity.ok(response);
  }

  /**
   * 평가 시험 리포트 PDF 다운로드
   * @param adminUserId 관리자 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 리포트 PDF
   */
  @GetMapping("/rooms/{roomId}/report/download")
  public ResponseEntity<byte[]> downloadRoomReportPdf(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID adminUserId,
      @PathVariable UUID roomId
  ) {
    byte[] pdfBytes = roomService.downloadRoomReportPdf(adminUserId, roomId);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_PDF);

    ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename("room_" + roomId + "_report.pdf", StandardCharsets.UTF_8)
                .build();
    headers.setContentDisposition(contentDisposition);

    return ResponseEntity.ok().headers(headers).body(pdfBytes);
  }

  /**
   * AI 선제 채점 시작 (비동기 루틴)
   * @param adminUserId 관리자 유저 아이디
   * @param roomId 평가 시험 아이디
   * @return 채점 시작 성공 여부
   */
  @PostMapping("/rooms/{roomId}/ai-grade")
  public ResponseEntity<Void> startAiGrading(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID adminUserId,
      @PathVariable UUID roomId
  ) {
    roomService.startAiGrading(adminUserId, roomId);

    // 비동기 처리이므로 즉시 202 Accepted 반환
    return ResponseEntity.accepted().build();
  }
}
