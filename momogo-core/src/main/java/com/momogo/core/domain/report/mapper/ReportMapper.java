package com.momogo.core.domain.report.mapper;

import com.momogo.core.domain.report.dto.response.PersonalReportResponse;
import com.momogo.core.domain.report.entity.PersonalReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// MapStruct 인터페이스
@Mapper(componentModel = "spring")
public interface ReportMapper {

  @Mapping(target = "accuracyRate", expression = "java(calculateAccuracyRate(personalReport))")
  PersonalReportResponse toResponse(PersonalReport personalReport);

  /*
   * 정답률 계산 로직. MapStruct 인터페이스 안에 default 메서드로 두면,
   * 자동 생성되는 구현체 안에서 이 메서드를 그대로 호출해줌
   * attemptedCount가 0(아직 아무 문제도 안 푼 날/주)이면 0으로 나누기가 되니까 0.0으로 처리.
   */
  default Double calculateAccuracyRate(PersonalReport personalReport) {
    Integer attempted = personalReport.getAttemptedCount();
    Integer solved = personalReport.getSolvedCount();

    if (attempted == null || attempted == 0 || solved == null) {
      return 0.0;
    }
    return (double) solved / attempted * 100;
  }
}
