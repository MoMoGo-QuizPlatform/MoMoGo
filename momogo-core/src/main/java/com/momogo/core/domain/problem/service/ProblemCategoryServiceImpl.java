package com.momogo.core.domain.problem.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.request.CategoryCreateRequest;
import com.momogo.core.domain.problem.dto.request.CategoryUpdateRequest;
import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.mapper.CategoryMapper;
import com.momogo.core.domain.problem.repository.ProblemCategoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    // 이름 중복 검사 (동시 요청 시 이 체크를 통과해도 DB 유니크 제약으로 최종 방어됨 - 아래 catch 참고)
    if (categoryRepository.existsByName(categoryName)) {
      throw new BusinessException(ProblemErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    try {
      ProblemCategory savedCategory = categoryRepository.saveAndFlush(
          ProblemCategory.create(categoryName));
      return categoryMapper.toResponse(savedCategory);
    } catch (DataIntegrityViolationException e) {
      if (isDuplicateNameViolation(e)) {
        log.warn("[ProblemCategoryService] 카테고리 이름 중복 제약 조건 위반 발생");
        throw new BusinessException(ProblemErrorCode.CATEGORY_NAME_DUPLICATED);
      }
      throw e;
    }
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

  /**
   * 카테고리 수정 (슈퍼 어드민 전용)
   */
  @Override
  @Transactional
  public CategoryResponse updateCategory(UUID categoryId, CategoryUpdateRequest request) {

    ProblemCategory category = categoryRepository.findById(categoryId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.CATEGORY_NOT_FOUND));

    if (categoryRepository.existsByNameAndIdNot(request.name(), categoryId)) {
      throw new BusinessException(ProblemErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    category.update(request.name());

    return categoryMapper.toResponse(category);
  }

  /**
   * DB 제약 조건 예외(DataIntegrityViolationException)가 카테고리 이름 유니크 제약(UQ_PROBLEM_CATEGORY_NAME) 위반인지 판단합니다.
   */
  private boolean isDuplicateNameViolation(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    String message = cause.getMessage();
    if (message == null) {
      return false;
    }
    return message.toLowerCase().contains("uq_problem_category_name");
  }
}
