package com.momogo.core.domain.room.exception;

import com.momogo.core.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {

  ROOM_NOT_FOUND(4001, "ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 평가시험 방입니다."),
  INVALID_TEST_TIME(4002, "INVALID_TEST_TIME", HttpStatus.BAD_REQUEST, "시험 시작 시간은 종료 시간 이전이어야 합니다."),
  ALREADY_ENDED(4003, "ROOM_ALREADY_ENDED", HttpStatus.BAD_REQUEST, "이미 종료된 시험입니다."),
  ROOM_USER_NOT_FOUND(4004, "ROOM_USER_NOT_FOUND", HttpStatus.NOT_FOUND, "응시 대상 유저 중 존재하지 않는 유저가 있습니다."),
  INVALID_ACCESS_BEFORE_START(4005, "INVALID_ACCESS_BEFORE_START", HttpStatus.BAD_REQUEST, "시험 시작 시간 전에는 문제를 조회할 수 없습니다."),
  NOT_ROOM_PARTICIPANT(4006, "NOT_ROOM_PARTICIPANT", HttpStatus.FORBIDDEN, "해당 평가 시험의 응시 대상자가 아닙니다."),
  PROBLEM_NOT_FOUND(4007, "PROBLEM_NOT_FOUND", HttpStatus.NOT_FOUND, "존재하지 않는 문제입니다."),
  REPORT_GENERATION_FAILED(4008, "REPORT_GENERATION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "리포트 PDF 문서 생성 중 서버 내부 오류가 발생했습니다."),
  DUPLICATE_ANSWER_SUBMITTED(4009, "DUPLICATE_ANSWER_SUBMITTED", HttpStatus.BAD_REQUEST, "동일한 문제에 대한 중복 답안 제출은 허용되지 않습니다."),
  REPORT_NOT_READY(4010, "REPORT_NOT_READY", HttpStatus.BAD_REQUEST, "아직 채점이 완료되지 않아 리포트가 준비되지 않았습니다."),
  AI_GRADING_IN_PROGRESS(4011, "AI_GRADING_IN_PROGRESS", HttpStatus.BAD_REQUEST, "현재 해당 시험방의 AI 채점이 진행 중입니다."),
  LOCK_ACQUISITION_FAILED(4012, "LOCK_ACQUISITION_FAILED", HttpStatus.SERVICE_UNAVAILABLE, "동시 요청 처리를 위한 락 획득에 실패했습니다. 잠시 후 다시 시도해주세요."),
  ALREADY_SUBMITTED(4013, "ALREADY_SUBMITTED", HttpStatus.CONFLICT, "이미 답안을 제출한 시험입니다.");

  private final int numeric;
  private final String errorKey;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public String getDomain() {
    return "ROOM";
  }

  @Override
  public String getCode() {
    return getDomain() + "-" + getErrorKey();
  }
}
