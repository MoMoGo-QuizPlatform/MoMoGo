package com.momogo.ai.config;

import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 관련 설정 클래스
 */
@Configuration
public class AiChatClientConfig {

  /**
   * 문제 생성 전용 ChatClient
   */
  @Bean
  @Qualifier("problemGenerationChatClient")
  public ChatClient problemGenerationChatClient(ChatClient.Builder builder) {

    return builder
        .defaultSystem("""
            당신은 교육 콘텐츠 제작 전문가입니다.
            주어진 참고자료를 기반으로 명확하고 정확한 문제를 출제합니다.
            문제는 모호함이 없어야 하며 정답은 참고자료 안에서 명확히 근거를 찾을 수 있어야 합니다.
            단, 문제의 답이 무조건 단답식으로 하나의 정답만 나올 수 있게 문제를 만들어 주셔야 합니다.
            예를 들어 JPA의 장점인 기능들을 말해봐라 이런 식이면 더티 체킹, 쓰기 지연 등
            복수 답이 나올 수 있으므로, JPA에서 변경이 발생했을 때 어떤 기능에 의해 자동으로 감지되는가?
            와 같은 단답식 정답이 나와야 합니다.
            """)
        // 생성은 다양성이 필요 -> temperature 조금 높게 설정
        .defaultOptions(GoogleGenAiChatOptions.builder().temperature(0.7).build())
        .build();
  }

  /**
   * 단답식 정답 검증 전용 ChatClient
   */
  @Bean
  @Qualifier("answerValidationChatClient")
  public ChatClient answerValidationChatClient(ChatClient.Builder builder) {

    return builder
        .defaultSystem("""
            당신은 문제와 정답을 검토하는 검수자입니다.
            주어진 문제와 정답이 하나의 명확한 개념/용어만 가리키는 단일 정답인지 판단합니다.
            정답에 쉼표, 슬래시, "및", "또는", "그리고" 등으로 나열된 복수 개념이 포함되어 있거나
            서술형으로 풀어썼지만 실질적으로 두 가지 이상의 답이 될 수 있다면 단일 정답이 아닙니다.
            판단 근거를 먼저 설명한 뒤, 결론을 내려주세요.
            """)
        // 판단 작업은 일관성이 중요 -> temperature 낮게 설정
        .defaultOptions(GoogleGenAiChatOptions.builder().temperature(0.1).build())
        .build();
  }

  /**
   * 문제 채점 전용 ChatClient
   */
  @Bean
  @Qualifier("problemGradingChatClient")
  public ChatClient problemGradingChatClient(ChatClient.Builder builder) {

    return builder
        .defaultSystem("""
            당신은 공정하고, 엄격한 채점관입니다.
            응시자의 답안이 모범 답안과 의미적으로 일치하는지 판단합니다.
            표현이 다르더라도 핵심 내용이 같으면 정답으로 인정합니다.
            핵심 내용이 빠지거나 틀린 경우에만 오답으로 판단합니다.
            """)
        // 채점은 일관성을 보장해야 하기에 조금 낮게 설정
        .defaultOptions(GoogleGenAiChatOptions.builder().temperature(0.2).build())
        .build();
  }

  /**
   * OpenAI 호출 타임아웃 설정
   */
  @Bean
  public RestClient.Builder restClientBuilder() {

    ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
        .withConnectTimeout(Duration.ofSeconds(5))  // 연결 5초
        .withReadTimeout(Duration.ofSeconds(30));   // 응답 30초

    ClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.detect().build(settings);

    return RestClient.builder().requestFactory(factory);
  }
}
