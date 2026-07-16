package com.momogo.api.problem;

import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import com.momogo.core.domain.problem.service.ProblemCategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class ProblemCategoryController {

  private final ProblemCategoryService problemCategoryService;

  /**
   * 문제 카테고리 목록 조회
   * @return 문제 카테고리 목록
   */
  @GetMapping
  public ResponseEntity<List<CategoryResponse>> getCategories() {

    return ResponseEntity
        .ok(problemCategoryService.getCategories());
  }
}
