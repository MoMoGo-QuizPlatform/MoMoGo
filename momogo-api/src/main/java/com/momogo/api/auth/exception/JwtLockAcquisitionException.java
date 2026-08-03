package com.momogo.api.auth.exception;

import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.common.exception.BusinessException;

import java.util.UUID;

public class JwtLockAcquisitionException extends BusinessException {

    public JwtLockAcquisitionException(UUID userId) {
        super(AuthErrorCode.LOCK_ACQUISITION_FAILED, "JWT 락 획득 타임아웃 발생 - userId: " + userId);
    }

    public JwtLockAcquisitionException(UUID userId, Throwable cause) {
        super(AuthErrorCode.LOCK_ACQUISITION_FAILED, "JWT 락 획득 중 인터럽트 발생 - userId: " + userId, cause);
    }
}
