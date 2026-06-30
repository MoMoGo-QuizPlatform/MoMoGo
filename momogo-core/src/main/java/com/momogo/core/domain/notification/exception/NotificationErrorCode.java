package com.momogo.core.domain.notification.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  NOTIFICATION_NOT_FOUND(5001, "NOT_FOUND", HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "NOTI";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}