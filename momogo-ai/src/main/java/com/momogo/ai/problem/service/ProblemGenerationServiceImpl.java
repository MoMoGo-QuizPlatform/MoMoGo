package com.momogo.ai.problem.service;

import com.momogo.ai.problem.dto.GeneratedProblemSetDto;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.service.ProblemGenerationService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI 문제 생성 서비스
 */
@Slf4j
@Service
public class ProblemGenerationServiceImpl implements ProblemGenerationService {

  private final ChatClient chatClient;

  public ProblemGenerationServiceImpl(
      @Qualifier("problemGenerationChatClient") ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  /**
   * 참고 자료 기반 LLM 통해 문제 생성
   *
   * @param referenceText 참고 자료
   * @param questionCount 생성할 문항 수
   * @return LLM이 생성한 문제 묶음
   */
  @Override
  public List<GeneratedProblemData> generateProblems(String referenceText, int questionCount) {

    String userPrompt = buildPrompt(referenceText, questionCount);

    try {
      GeneratedProblemSetDto result = chatClient.prompt()
          .user(userPrompt)
          .call()
          .entity(GeneratedProblemSetDto.class);

      // momogo-ai 내부 파싱용 DTO -> momogo-core용 DTO로 변환
      return result.items()
          .stream()
          .map(dto -> new GeneratedProblemData(
              dto.name(),
              dto.content(),
              dto.correctAnswer(),
              dto.explanation()))
          .toList();
    } catch (Exception e) {
      log.error("AI 문제 생성 실패. referenceText 길이={}, questionCount={}", referenceText.length(), questionCount, e);

      throw new BusinessException(ProblemErrorCode.AI_GENERATION_FAILED);
    }
  }

  /**
   * 프롬프트 구성 메서드
   *
   * @param referenceText 참고 자료
   * @param questionCount 문제 수
   */
  private String buildPrompt(String referenceText, int questionCount) {

    return """
        아래 참고자료를 바탕으로 정확히 %d개의 주관식 문제를 생성해주세요.
        
        [참고자료]
        %s
        
        [생성 규칙]
        - 각 문제는 참고자료의 서로 다른 부분을 다뤄야 합니다. (중복 금지)
        - 정답은 참고자료에서 명확히 근거를 찾을 수 있어야 합니다.
        - 해설은 왜 그 답이 맞는지 참고자료를 인용하여 2~3문장으로 설명해주세요.
        - 문제 난이도는 참고자료를 학습한 사람이라면 풀 수 있는 수준으로 만들어주세요.
        """
        .formatted(questionCount, referenceText);
  }
}
