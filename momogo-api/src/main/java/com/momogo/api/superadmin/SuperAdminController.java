package com.momogo.api.superadmin;

import com.momogo.core.domain.problem.dto.request.CategoryCreateRequest;
import com.momogo.core.domain.problem.dto.request.CategoryUpdateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemUpdateRequest;
import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import com.momogo.core.domain.problem.dto.response.ProblemDetailResponse;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.service.ProblemCategoryService;
import com.momogo.core.domain.problem.service.ProblemService;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.dto.response.SpaceResponse;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.mapper.SpaceMapper;
import com.momogo.core.domain.space.service.SpaceService;
import com.momogo.core.domain.user.dto.request.UserBannedRequest;
import com.momogo.core.domain.user.dto.request.UserPageRequest;
import com.momogo.core.domain.user.dto.response.CursorResponse;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

  private final SpaceService spaceService;
  private final SpaceMapper spaceMapper;
  private final UserService userService;
  private final ProblemCategoryService problemCategoryService;
  private final ProblemService problemService;

  /**
   * 전체 공간 조회
   * @param pageable 페이징 정보
   * @return 전체 공간 정보
   */
  @GetMapping("/spaces")
  public ResponseEntity<Page<SpaceResponse>> getAllSpaces(
      @PageableDefault(size = 10) Pageable pageable
  ) {
    return ResponseEntity.ok(spaceService.getAllSpaces(pageable));
  }

  /**
   * 공간 강제 수정 (SUPER_ADMIN 전용)
   * @param spaceId 공간 ID
   * @param request 공간 수정 요청 DTO
   * @return 수정된 공간 정보
   */
  @PutMapping("/spaces/{spaceId}")
  public ResponseEntity<SpaceResponse> forceUpdateSpace(
      @PathVariable UUID spaceId,
      @Valid @RequestBody SpaceUpdateRequest request
  ) {
    Space space = spaceService.forceUpdateSpace(spaceId, request);
    return ResponseEntity.ok(spaceMapper.toResponse(space));
  }

  /**
   * 공간 강제 폐쇄 (SUPER_ADMIN 전용)
   * @param spaceId 공간 ID
   */
  @DeleteMapping("/spaces/{spaceId}")
  public ResponseEntity<Void> forceDeleteSpace(@PathVariable UUID spaceId) {
    spaceService.forceDeleteSpace(spaceId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 전체 문제 조회 (SUPER_ADMIN 전용, 공간 무관, 정답 포함)
   * @param pageable 페이징 정보
   * @return 전체 문제 정보
   */
  @GetMapping("/problems")
  public ResponseEntity<Page<ProblemDetailResponse>> getAllProblems(
      @PageableDefault(size = 10) Pageable pageable
  ) {
    return ResponseEntity.ok(problemService.getAllProblems(pageable));
  }

  /**
   * 문제 강제 수정 (SUPER_ADMIN 전용)
   * @param problemId 문제 ID
   * @param request   문제 수정 요청 DTO
   * @return 수정된 문제 정보
   */
  @PutMapping("/problems/{problemId}")
  public ResponseEntity<ProblemResponse> forceUpdateProblem(
      @PathVariable UUID problemId,
      @Valid @RequestBody ProblemUpdateRequest request
  ) {
    return ResponseEntity.ok(problemService.updateProblemAsSuperAdmin(problemId, request));
  }

  /**
   * 문제 강제 삭제 (SUPER_ADMIN 전용)
   * @param problemId 문제 ID
   */
  @DeleteMapping("/problems/{problemId}")
  public ResponseEntity<Void> forceDeleteProblem(@PathVariable UUID problemId) {
    problemService.deleteProblemAsSuperAdmin(problemId);
    return ResponseEntity.noContent().build();
  }


  /**
   * 전체 유저 목록을 페이징(커서 기반) 조회합니다. (SUPER_ADMIN 전용)
   *
   * @param request 검색 조건 및 페이징 설정 DTO
   * @return 커서 정보가 포함된 페이징된 유저 목록 응답 객체
   */
  @GetMapping("/users")
  public ResponseEntity<CursorResponse<UserResponse>> findAllUsers(
      @Valid @ModelAttribute UserPageRequest request
  ) {
    return ResponseEntity.ok(userService.findAllUsers(request));
  }

  /**
   * 특정 유저의 정지(밴) 상태를 업데이트합니다. (SUPER_ADMIN 전용)
   *
   * @param userId  정지 상태를 변경할 대상 유저 식별자
   * @param request 정지 여부(banned) 설정 DTO
   * @return 변경 완료된 회원 정보 DTO
   */
  @PatchMapping("/users/{userId}/ban")
  public ResponseEntity<UserResponse> updateUserBannedStatus(
      @PathVariable UUID userId,
      @Valid @RequestBody UserBannedRequest request
  ) {
    return ResponseEntity.ok(userService.updateUserBannedStatus(userId, request.banned()));
  }


  /**
   * 문제 카테고리 생성
   * @param request 카테고리 생성 요청 DTO
   * @return 생성된 카테고리 정보
   */
  @PostMapping("/categories")
  public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CategoryCreateRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(problemCategoryService.createCategory(request));
  }

  /**
   * 문제 카테고리 수정
   * @param categoryId 카테고리 아이디
   * @param request 카테고리 수정 요청 DTO
   * @return 수정된 카테고리 정보
   */
  @PatchMapping("/categories/{categoryId}")
  public ResponseEntity<CategoryResponse> updateCategory(
      @PathVariable UUID categoryId,
      @RequestBody @Valid CategoryUpdateRequest request) {

    return ResponseEntity
        .ok(problemCategoryService.updateCategory(categoryId, request));
  }
}
