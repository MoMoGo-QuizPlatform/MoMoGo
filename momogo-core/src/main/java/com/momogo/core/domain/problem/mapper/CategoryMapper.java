package com.momogo.core.domain.problem.mapper;

import com.momogo.core.domain.problem.dto.response.CategoryResponse;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import org.mapstruct.Mapper;

/**
 * 카테고리 도메인 변환 Mapper
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

  CategoryResponse toResponse(ProblemCategory category);
}
