package com.momogo.api.problem;

import com.momogo.core.domain.problem.dto.request.ProblemCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemUpdateRequest;
import com.momogo.core.domain.problem.dto.response.ProblemCursorResponse;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.service.ProblemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
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
      @RequestParam UUID categoryId,
      @RequestParam(required = false) String nameKeyword,
      @RequestParam(required = false) String contentKeyword,
      @RequestParam(required = false) OffsetDateTime cursor,
      @RequestParam(required = false) UUID cursorId,
      @RequestParam(defaultValue = "10") @Max(100) int size) {

    return ResponseEntity.ok(
        problemService.getProblems(
            spaceId,
            categoryId,
            nameKeyword,
            contentKeyword,
            cursor,
            cursorId,
            size
        )
    );
  }

  /**
   * 문제 단건 조회
   */
  @GetMapping("/{problemId}")
  public ResponseEntity<ProblemResponse> getProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId) {

    return ResponseEntity
        .ok(problemService.getProblem(problemId));
  }

  /**
   * 문제 수정
   */
  @PutMapping("/{problemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ProblemResponse> updateProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId,
      @RequestBody @Valid ProblemUpdateRequest request) {

    return ResponseEntity
        .ok(problemService.updateProblem(spaceId, problemId, request));
  }

  @DeleteMapping("/{problemId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteProblem(
      @PathVariable UUID spaceId,
      @PathVariable UUID problemId) {

    problemService.deleteProblem(spaceId, problemId);

    return ResponseEntity.noContent().build();
  }

  // 문제 제출은 아직 서비스 미완 및 협의가 필요한 부분이라
  // 추가적으로 팀원들과 회의를 통해서 추가 예정
}
