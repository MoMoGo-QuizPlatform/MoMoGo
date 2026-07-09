package com.momogo.core.domain.problem.service;

import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.entity.Problem;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.problem.entity.ProblemCounters;
import com.momogo.core.domain.problem.mapper.ProblemMapper;
import com.momogo.core.domain.problem.repository.ProblemCategoryRepository;
import com.momogo.core.domain.problem.repository.ProblemCountersRepository;
import com.momogo.core.domain.problem.repository.ProblemRepository;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.repository.SpaceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProblemPersister {

  private final ProblemRepository problemRepository;
  
  private final ProblemCountersRepository countersRepository;
  
  private final ProblemMapper problemMapper;

  private final SpaceRepository spaceRepository;

  private final ProblemCategoryRepository categoryRepository;

  /**
   * 문제 저장 + 통계 초기화만 수행 (LLM 호출 X)
   */
  @Transactional
  public List<ProblemResponse> saveGeneratedProblems(
      UUID spaceId,
      UUID categoryId,
      List<GeneratedProblemData> generated) {

    Space space = spaceRepository.getReferenceById(spaceId);

    ProblemCategory category = categoryRepository.getReferenceById(categoryId);

    // 생성된 문제들 -> 엔티티로 변환해서 일괄 저장
    List<Problem> savedProblems = generated.stream()
        .map(dto -> problemRepository.save(
            Problem.create(
                category,
                space,
                dto.name(),
                dto.content(),
                dto.explanation(),
                dto.correctAnswer())))
        .toList();

    // 각 문제마다 통계 레코드 초기화
    savedProblems.forEach(p -> countersRepository.save(ProblemCounters.init(p)));

    return problemMapper.toResponseList(savedProblems);
  }
}
