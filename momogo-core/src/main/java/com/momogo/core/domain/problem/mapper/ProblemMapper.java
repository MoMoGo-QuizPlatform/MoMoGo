package com.momogo.core.domain.problem.mapper;

import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.dto.response.ProblemSolveResponse;
import com.momogo.core.domain.problem.entity.Problem;
import com.momogo.core.domain.problem.entity.ProblemCounters;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 문제 도메인 변환 Mapper
 */
@Mapper(componentModel = "spring")
public interface ProblemMapper {

  // Problem → ProblemResponse 변환
  @Mapping(source = "space.id", target = "spaceId")
  @Mapping(source = "category.id", target = "categoryId")
  @Mapping(source = "category.name", target = "categoryName")
  ProblemResponse toResponse(Problem problem);

  // List<Problem> → List<ProblemResponse> 변환
  List<ProblemResponse> toResponseList(List<Problem> problems);
}
