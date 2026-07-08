package com.momogo.core.domain.problem.service;


import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import java.util.List;

/**
 * AI 문제 생성 서비스 인터페이스
 */
public interface ProblemGenerationService {

  /**
   * 참고자료 기반 LLM 통해 문제 생성
   * @param referenceText   참고 자료
   * @param questionCount   생성할 문항 수
   * @return                LLM이 생성한 문제 묶음
   */
  List<GeneratedProblemData> generateProblems(String referenceText, int questionCount);
}
