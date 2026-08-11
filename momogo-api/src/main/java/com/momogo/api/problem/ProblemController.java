package com.momogo.api.problem;

import com.momogo.core.domain.problem.dto.request.ProblemAiCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemSearchRequest;
import com.momogo.core.domain.problem.dto.request.ProblemSolveRequest;
import com.momogo.core.domain.problem.dto.request.ProblemUpdateRequest;
import com.momogo.core.domain.problem.dto.response.ProblemCursorResponse;
import com.momogo.core.domain.problem.dto.response.ProblemDetailResponse;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.dto.response.ProblemSolveResponse;
import com.momogo.core.domain.problem.service.ProblemService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceId}/problems")
public class ProblemController {

  private final ProblemService problemService;

  /**
   * 문제 직접 생성 (권한: ADMIN)
   */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProblemResponse> createProblem(
      @PathVariable UUID spaceId,
      @RequestBody @Valid ProblemCreateRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(problemService.createProblem(spaceId, request));
  }

  /**
   * 문제 목록 조회 (커서 페이지네이션)
   */
  @GetMapping
  public ResponseEntity<ProblemCursorResponse> getProblems(
      @PathVariable UUID spaceId,
      @Valid ProblemSearchRequest request,
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId) {

    return ResponseEntity.ok(
        problemService.getProblems(
            spaceId,
            request.categoryId(),
            request.nameKeyword(),
            request.contentKeyword(),
            request.cursor(),
            request.cursorId(),
            request.size(),
            userId
        )
    );
  }

  /**
   * 문제 단건 조회 (편집용, 정답 포함)
   */
  @GetMapping("/{problemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProblemDetailResponse> getProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId) {

    return ResponseEntity
        .ok(problemService.getProblem(spaceId, problemId));
  }

  /**
   * 문제 수정
   */
  @PatchMapping("/{problemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProblemResponse> updateProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId,
      @RequestBody @Valid ProblemUpdateRequest request) {

    return ResponseEntity
        .ok(problemService.updateProblem(spaceId, problemId, request));
  }

  /**
   * 문제 삭제
   */
  @DeleteMapping("/{problemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId) {

    problemService.deleteProblem(spaceId, problemId);

    return ResponseEntity.noContent().build();
  }

  /**
   * AI 기반 문제 자동 생성 (ADMIN 전용)
   */
  @PostMapping("/ai")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<ProblemResponse>> createProblemsByAi(
      @PathVariable UUID spaceId,
      @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
      @RequestBody @Valid ProblemAiCreateRequest request) {

    UUID key = (idempotencyKey != null) ? idempotencyKey : UUID.randomUUID();
    List<ProblemResponse> response = problemService.createProblemsByAi(spaceId, key, request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  /**
   * 문제 제출 및 채점
   */
  @PostMapping("/{problemId}/solve")
  public ResponseEntity<ProblemSolveResponse> solveProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId,
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @RequestBody @Valid ProblemSolveRequest request) {

    return ResponseEntity
        .ok(problemService.solveProblem(spaceId, problemId, userId, request));
  }
}
