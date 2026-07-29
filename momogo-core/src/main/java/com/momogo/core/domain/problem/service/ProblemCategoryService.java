package com.momogo.core.domain.problem.service;

import com.momogo.core.domain.problem.dto.request.CategoryCreateRequest;
import com.momogo.core.domain.problem.dto.request.CategoryUpdateRequest;
import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import java.util.List;
import java.util.UUID;

public interface ProblemCategoryService {

  /**
   * 카테고리 생성 (슈퍼 어드민 전용)
   */
  CategoryResponse createCategory(CategoryCreateRequest request);

  /**
   * 카테고리 전체 목록 조회
   */
  List<CategoryResponse> getCategories();

  /**
   * 카테고리 수정 (슈퍼 어드민 전용)
   */
  CategoryResponse updateCategory(UUID categoryId, CategoryUpdateRequest request);
}
