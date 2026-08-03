package com.momogo.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RealtimeErrorCode implements ErrorCode {

  REDIS_PUBLISH_FAILED(8001, "REDIS_PUBLISH_FAILED", HttpStatus.SERVICE_UNAVAILABLE, "실시간 메시지 발행 중 오류가 발생했습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "REALTIME";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
