package com.momogo.ai.config;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AI 문제 검증 / 재생성 병렬 처리용 스레드풀 설정
 * ForkJoinPool.commonPool() 공용 풀을 쓰면 다른 병렬 작업과 자원 경합 생겨
 * 문제 원인 추적이 어려워지므로 전용 풀로 분리
 */
@Configuration
public class AiExecutorConfig {

  /**
   * 문제 정답 검증 / 재생성 LLM 호출 전용 Executor
   * 네트워크 응답 대기가 대부분이라 cpu 코어 수보다 넉넉하게 설정
   */
  @Bean
  @Qualifier("problemValidationExecutor")
  public Executor problemValidationExecutor() {

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(5);                          // 평소 유지 스레드 수

    executor.setMaxPoolSize(10);                          // 순간 몰릴 때 최대 확장

    executor.setThreadNamePrefix("problem-validate-");    // 식별용

    return executor;
  }
}
