package com.momogo.realtime.common.exception;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(BusinessException.class)
    public void handleWebsocketException(BusinessException e) {
        ErrorCode code = e.getErrorCode();

        log.warn("[WebSocket FAIL] code={}, message={}",
                code.getCode(), e.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    public void handleException(Exception e) {
        log.error("[WebSocket UNKNOWN ERROR] message={}", e.getMessage(), e);
    }
}
