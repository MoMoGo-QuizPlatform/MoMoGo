package com.momogo.ai.problem.service;

import com.momogo.ai.problem.dto.AnswerValidationDto;
import com.momogo.ai.problem.dto.GeneratedProblemDto;
import com.momogo.ai.problem.dto.GeneratedProblemSetDto;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.service.ProblemGenerationService;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * AI 문제 생성 서비스
 */
@Slf4j
@Service
public class ProblemGenerationServiceImpl implements ProblemGenerationService {

  private final ChatClient chatClient;

  private final ChatClient validationChatClient;

  /**
   * 정답에 복수 개념 나열 신호 (구분자 / 접속어)가 있는지 걸러내는 1차 휴리스틱
   * 걸리는 경우에만 LLM 재검증 호출 -> 비용 절감
   */
  private static final Pattern MULTI_ANSWER_PATTERN = Pattern.compile("[,/·、]| 및 | 또는 | 그리고 ");

  public ProblemGenerationServiceImpl(
      @Qualifier("problemGenerationChatClient") ChatClient chatClient,
      @Qualifier("answerValidationChatClient") ChatClient validationChatClient) {

    this.chatClient = chatClient;
    this.validationChatClient = validationChatClient;
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

      // 각 문제 정답이 단일 정답인지 검증하고, 아니면 1회 재생성
      // momogo-ai 내부 파싱용 DTO -> momogo-core용 DTO로 변환
      return result.items()
          .stream()
          .map(dto -> ensureSingleAnswer(dto, referenceText))
          .map(dto -> new GeneratedProblemData(
              dto.name(),
              dto.content(),
              dto.correctAnswer(),
              dto.explanation()))
          .toList();
    } catch (TransientAiException | NonTransientAiException e) {

      log.error("LLM API 호출 실패. referenceText 길이={}, questionCount={}", referenceText.length(),
          questionCount, e);

      throw new BusinessException(ProblemErrorCode.AI_GENERATION_FAILED);
    } catch (Exception e) {

      log.error("LLM 응답 파싱 실패. referenceText 길이={}, questionCount={}", referenceText.length(),
          questionCount, e);

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

  /**
   * 정답 단일성 검증 후 -> 실패하면 해당 문제 1개만 재생성 (최대 1회)
   * 재생성 결과도 단일 정답이 아니면 생성 실패로 처리
   */
  private GeneratedProblemDto ensureSingleAnswer(GeneratedProblemDto dto, String referenceText) {

    if (isSingleAnswer(dto)) {
      return dto;
    }

    log.warn("복수 정답 의심 문제 감지, 재생성 시도. name={}, correctAnswer={}", dto.name(), dto.correctAnswer());

    GeneratedProblemDto regenerated = regenerateSingleProblem(referenceText);

    if (!isSingleAnswer(regenerated)) {
      throw new BusinessException(ProblemErrorCode.AI_GENERATION_FAILED);
    }

    return regenerated;
  }

  /**
   * 정답 단일성 검증
   * 정규식 휴리스틱을 먼저 거치고, 의심되는 경우에만 LLM 재검증을 호출한다.
   */
  private boolean isSingleAnswer(GeneratedProblemDto dto) {

    if (!MULTI_ANSWER_PATTERN.matcher(dto.correctAnswer()).find()) {
      return true;
    }

    String validationPrompt = """
        [문제]
        %s
        
        [정답]
        %s
        
        위 정답이 하나의 명확한 개념/용어만 가리키는 단일 정답인지 판단해주세요.
        """
        .formatted(dto.content(), dto.correctAnswer());

    AnswerValidationDto validation = validationChatClient
        .prompt()
        .user(validationPrompt)
        .call()
        .entity(AnswerValidationDto.class);

    return validation.singleAnswer();
  }

  /**
   * 문제 1개만 다시 생성 (검증 실패 시 재시도용)
   */
  private GeneratedProblemDto regenerateSingleProblem(String referenceText) {

    String prompt = buildPrompt(referenceText, 1);

    GeneratedProblemSetDto result = chatClient.prompt()
        .user(prompt)
        .call()
        .entity(GeneratedProblemSetDto.class);

    return result.items().get(0);
  }
}
