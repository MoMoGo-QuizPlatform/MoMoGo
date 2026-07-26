package com.momogo.core.domain.problem.mapper;

import com.momogo.core.domain.problem.dto.response.ProblemDetailResponse;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.entity.Problem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 문제 도메인 변환 Mapper
 */
@Mapper(componentModel = "spring")
public interface ProblemMapper {

  // Problem → ProblemResponse 변환
  // isSolved는 Problem 엔티티에 없는 값(유저별 풀이 이력)이라 서비스 레이어에서 별도로 채워 넣는다.
  @Mapping(source = "space.id", target = "spaceId")
  @Mapping(source = "category.id", target = "categoryId")
  @Mapping(source = "category.name", target = "categoryName")
  @Mapping(target = "isSolved", ignore = true)
  ProblemResponse toResponse(Problem problem);

  // List<Problem> → List<ProblemResponse> 변환
  List<ProblemResponse> toResponseList(List<Problem> problems);

  // Problem -> ProblemDetailResponse 변환 (편집용, 정답 포함)
  @Mapping(source = "space.id", target = "spaceId")
  @Mapping(source = "category.id", target = "categoryId")
  @Mapping(source = "category.name", target = "categoryName")
  ProblemDetailResponse toDetailResponse(Problem problem);
}
