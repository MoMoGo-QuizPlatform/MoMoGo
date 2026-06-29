package com.momogo.core.domain.problem.service.impl;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.request.CategoryCreateRequest;
import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.mapper.CategoryMapper;
import com.momogo.core.domain.problem.repository.ProblemCategoryRepository;
import com.momogo.core.domain.problem.service.ProblemCategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemCategoryServiceImpl implements ProblemCategoryService {

  private final ProblemCategoryRepository categoryRepository;

  private final CategoryMapper categoryMapper;

  /**
   * 카테고리 생성 (슈퍼 어드민 전용)
   */
  @Override
  @Transactional
  public CategoryResponse createCategory(CategoryCreateRequest request) {

    String categoryName = request.name();

    // 이름 중복 검사
    if (categoryRepository.existsByName(categoryName)) {
      throw new BusinessException(ProblemErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    ProblemCategory savedCategory = categoryRepository.save(ProblemCategory.create(categoryName));

    return categoryMapper.toResponse(savedCategory);
  }

  /**
   * 카테고리 전체 목록 조회
   */
  @Override
  public List<CategoryResponse> getCategories() {

    return categoryRepository.findAllByOrderByNameAsc()
        .stream()
        .map(categoryMapper::toResponse)
        .toList();
  }
}
