package com.momogo.ai.grading.listener;

import com.momogo.ai.problem.dto.AiGradingResultDto;
import com.momogo.ai.problem.dto.AiGradingResultSetDto;
import com.momogo.core.domain.room.entity.UserRoomAnswer;
import com.momogo.core.domain.room.event.StartAiGradingEvent;
import com.momogo.core.domain.room.repository.UserRoomAnswerRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class AiGradingEventListener {

  private final ChatClient chatClient;
  private final UserRoomAnswerRepository userRoomAnswerRepository;

  public AiGradingEventListener(
      @Qualifier("problemGradingChatClient") ChatClient chatClient,
      UserRoomAnswerRepository userRoomAnswerRepository
  ) {
    this.chatClient = chatClient;
    this.userRoomAnswerRepository = userRoomAnswerRepository;
  }

  @Async
  @EventListener
  @Transactional
  public void handleStartAiGradingEvent(StartAiGradingEvent event) {

    log.info("[AiGradingListener] 비동기 AI 채점 백그라운드 작업 시작 - roomId: {}", event.roomId());

    // 해당 시험방의 모든 응시자가 제출한 답안 리스트 전체 조회
    List<UserRoomAnswer> answers = userRoomAnswerRepository.findByRoomProblemRoomId(event.roomId());
    log.info("[AiGradingListener] 조회된 답안지 개수: {}", answers.size());
    if (answers.isEmpty()) {
      log.info("[AiGradingListener] 채점 대상 제출 답안지가 존재하지 않습니다. - roomId: {}", event.roomId());
      return;
    }

    // 주관식 채점 요청 프롬프트 텍스트 조립
    String prompt = buildGradingPrompt(answers);
    log.info("[AiGradingListener] Gemini API 호출용 프롬프트 생성 완료");

    try {
      // Gemini 호출 및 Structured Output 자바 객체 매핑
      log.info("[AiGradingListener] Gemini API 호출을 시작합니다. (spring.ai.google.genai.api-key 확인됨)");
      
      AiGradingResultSetDto results = chatClient.prompt()
          .user(prompt)
          .call()
          .entity(AiGradingResultSetDto.class);

      log.info("[AiGradingListener] Gemini API 응답을 성공적으로 수신했습니다: {}", results);

      // 결과를 빠르게 매핑하기 위해 Map 변환
      Map<UUID, AiGradingResultDto> resultMap = results.items().stream()
          .collect(Collectors.toMap(AiGradingResultDto::answerId, r -> r));

      // DB 답안지에 일괄 채점 판정값(isCorrect) 갱신
      int gradedCount = 0;
      for (UserRoomAnswer answer : answers) {
        AiGradingResultDto result = resultMap.get(answer.getId());
        if (result != null) {
          answer.grade(result.isCorrect());
          gradedCount++;
        }
      }

      userRoomAnswerRepository.saveAll(answers);
      log.info("[AiGradingListener] 비동기 AI 채점 완료 - roomId: {}, 채점 성공 건수: {}", event.roomId(), gradedCount);

    } catch (Throwable t) {
      log.error("[AiGradingListener] Gemini AI 채점 중 예외/에러 발생 - roomId: {}", event.roomId(), t);
    }
  }

  /**
   * 문항별 답안 채점 요청 프롬프트 텍스트 조립
   * @param answers 답안 리스트
   * @return 요청 프롬프트
   */
  private String buildGradingPrompt(List<UserRoomAnswer> answers) {
    StringBuilder sb = new StringBuilder();
    sb.append("아래 제공된 응시자들의 답안 목록을 개별 검토하여 정답 여부(isCorrect)를 공정하고 정확하게 판별해주세요.\n");
    sb.append("주관식 및 서술형 문항이므로 모범 정답(correctAnswer)과 응시자의 답안(userAnswer)의 글자가 완전히 똑같지 않더라도, 의미상 동의어이거나 핵심 내용 단어가 포함되어 있다면 과감하게 정답(true)으로 인정해주세요.\n\n");

    sb.append("[채점 대상 답안 리스트]\n");
    for (UserRoomAnswer ans : answers) {
      sb.append(String.format("- [Answer ID]: %s\n", ans.getId()));
      sb.append(String.format("  [문제명]: %s\n", ans.getRoomProblem().getName()));
      sb.append(String.format("  [문제 내용]: %s\n", ans.getRoomProblem().getContent()));
      sb.append(String.format("  [모범 정답]: %s\n", ans.getRoomProblem().getCorrectAnswer()));
      sb.append(String.format("  [응시자 제출 답]: %s\n\n", ans.getUserAnswer()));
    }
    return sb.toString();
  }

}
