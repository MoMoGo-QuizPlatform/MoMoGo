package com.momogo.core.domain.problem.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 문제 도메인 전용 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum ProblemErrorCode implements ErrorCode {

  PROBLEM_NOT_FOUND(3001, "PROBLEM_NOT_FOUND",
      HttpStatus.NOT_FOUND, "해당 문제를 찾을 수 없습니다."),

  CATEGORY_NOT_FOUND(3002, "CATEGORY_NOT_FOUND",
      HttpStatus.NOT_FOUND, "해당 카테고리를 찾을 수 없습니다."),

  CATEGORY_NAME_DUPLICATED(3003, "CATEGORY_NAME_DUPLICATED",
      HttpStatus.CONFLICT, "이미 존재하는 카테고리 이름입니다."),

  CATEGORY_HAS_PROBLEMS(3004, "CATEGORY_HAS_PROBLEMS",
      HttpStatus.CONFLICT, "해당 카테고리에 문제가 존재하여 삭제할 수 없습니다."),

  SPACE_NOT_FOUND(3005, "SPACE_NOT_FOUND",
      HttpStatus.NOT_FOUND, "해당 공간을 찾을 수 없습니다."),

  PROBLEM_NOT_IN_SPACE(3006, "PROBLEM_NOT_IN_SPACE",
      HttpStatus.NOT_FOUND, "해당 공간에 속한 문제가 아닙니다."),

  PROBLEM_FIELD_BLANK(3007, "PROBLEM_FIELD_BLANK",
      HttpStatus.BAD_REQUEST, "수정할 필드 값은 공백일 수 없습니다."),

  INVALID_CURSOR(3008, "INVALID_CURSOR",
      HttpStatus.BAD_REQUEST, "커서 값이 올바르지 않습니다. cursor와 cursorId는 함께 존재하거나 함께 없어야 합니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "PROBLEM";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
