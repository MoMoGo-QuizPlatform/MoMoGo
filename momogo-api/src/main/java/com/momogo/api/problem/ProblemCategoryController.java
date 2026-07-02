package com.momogo.api.problem;

import com.momogo.core.domain.problem.dto.request.CategoryCreateRequest;
import com.momogo.core.domain.problem.dto.request.CategoryUpdateRequest;
import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import com.momogo.core.domain.problem.service.ProblemCategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class ProblemCategoryController {

  private final ProblemCategoryService problemCategoryService;

  /**
   * 카테고리 생성 (권한: SUPER_ADMIN)
   */
  @PostMapping
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<CategoryResponse> createCategory(@RequestBody @Valid CategoryCreateRequest request) {

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(problemCategoryService.createCategory(request));
  }

  @PatchMapping("/{categoryId}")
  @PreAuthorize("hasRole('SUPER_ADMIN')")
  public ResponseEntity<CategoryResponse> updateCategory(
      @PathVariable UUID categoryId,
      @RequestBody @Valid CategoryUpdateRequest request) {

    return ResponseEntity
        .ok(problemCategoryService.updateCategory(categoryId, request));
  }

  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getCategories() {

    return ResponseEntity
        .ok(problemCategoryService.getCategories());
  }
}
