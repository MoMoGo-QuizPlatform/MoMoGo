package com.momogo.core.domain.notification.exception;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.ErrorCode;

public class NotificationException extends BusinessException {

  public NotificationException(ErrorCode errorCode) {
    super(errorCode);
  }

  public NotificationException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
