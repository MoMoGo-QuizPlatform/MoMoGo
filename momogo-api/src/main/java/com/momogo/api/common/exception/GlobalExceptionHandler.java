package com.momogo.api.common.exception;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.ErrorCode;
import com.momogo.core.common.exception.ErrorResponse;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String INCORRECT_PARAMETER = "'%s' 파라미터 값이 올바르지 않습니다. (허용된 값: %s)";
    private static final String INVALID_PARAMETER_FORMAT = "'%s' 파라미터의 형식이 올바르지 않습니다.";
    private static final String INVALID_REQUEST = "잘못된 접근입니다.";
    private static final String MISSING_PARAMETER = "'%s' 필수 파라미터가 누락되었습니다.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode code = e.getErrorCode();
        log.error("비즈니스 예외 발생 : code={}, message={}, path={}", code.getCode(), code.getMessage(), request.getRequestURI());

        ErrorResponse response;
        if (e.getHint() != null) {
            response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI(), e.getHint());
        } else {
            response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI());
        }

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (message.contains("uq_problem_category_name")) {
            ErrorCode code = ProblemErrorCode.CATEGORY_NAME_DUPLICATED;
            log.warn("카테고리 이름 중복 : path={}", request.getRequestURI());
            return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.of(code, code.getMessage(), request.getRequestURI()));
        }

        ErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        log.error("데이터 무결성 예외 발생 : message={}, path={}", e.getMessage(), request.getRequestURI(), e);
        return ResponseEntity.status(code.getHttpStatus())
            .body(ErrorResponse.of(code, code.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        List<ErrorResponse.Detail> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.Detail(
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()
                ))
                .toList();

        log.error("유효성 검사 실패 : code={}, message={}, path={}, details={}", code.getCode(), code.getMessage(), request.getRequestURI(), details);

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI(), details);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;

        String detailsMessage;
        if (e.getRequiredType() != null && e.getRequiredType().isEnum()) {
            String allowedValues = Arrays.toString(e.getRequiredType().getEnumConstants());
            detailsMessage = String.format(INCORRECT_PARAMETER, e.getName(), allowedValues);
        } else {
            detailsMessage = String.format(INVALID_PARAMETER_FORMAT, e.getName());
        }

        List<ErrorResponse.Detail> details = List.of(new ErrorResponse.Detail(e.getName(), detailsMessage, e.getValue()));
        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException e, HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;
        String detailsMessage = String.format(MISSING_PARAMETER, e.getParameterName());

        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(e.getParameterName(), detailsMessage, "null")
        );

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);
        log.error("필수 파라미터 누락 : code={}, message={}, path={}, details={}",
                code.getCode(), code.getMessage(), request.getRequestURI(), details);

        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INVALID_INPUT;
        String headerName = e.getHeaderName();
        String errorMessage = String.format("필수 헤더 '%s'가 누락되었습니다.", headerName);

        log.warn("필수 헤더 누락: code={}, message={}, path={}",
                code.getCode(), errorMessage, request.getRequestURI());

        List<ErrorResponse.Detail> details = List.of(
                new ErrorResponse.Detail(headerName, errorMessage, null)
        );

        ErrorResponse response = ErrorResponse.of(code, INVALID_REQUEST, request.getRequestURI(), details);
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
        AuthenticationException e, HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.UNAUTHORIZED;
        log.warn("인증 예외 발생: code={}, message={}, path={}",
            code.getCode(), e.getMessage(), request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
        AccessDeniedException e, HttpServletRequest request
    ) {
        ErrorCode code = GlobalErrorCode.FORBIDDEN;
        log.warn("인가(권한) 예외 발생 : code={}, message={}, path={}",
            code.getCode(), e.getMessage(), request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(code, e.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        ErrorCode code = GlobalErrorCode.INTERNAL_SERVER_ERROR;
        log.error("서버 내부 오류 발생 : code={}, message={}, path={}",
                code.getCode(), e.getMessage(), request.getRequestURI(), e);

        ErrorResponse response = ErrorResponse.of(code, code.getMessage(), request.getRequestURI());
        return ResponseEntity.status(code.getHttpStatus()).body(response);
    }
}
