package com.momogo.core.domain.report.service;

import com.momogo.core.domain.report.dto.response.PersonalReportResponse;
import com.momogo.core.domain.report.dto.response.SpaceRankingResponse;
import com.momogo.core.domain.report.entity.ReportType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportService {

  // 개인 대시보드: 특정 기준일(Daily면 당일, Weekly면 그 주 시작일)의 리포트 조회
  PersonalReportResponse getPersonalReport(UUID userId, ReportType reportType, LocalDate reportDate);

  // 공간 대시보드: 이번 주 정답 수 기준 랭킹 조회
  List<SpaceRankingResponse> getSpaceRanking(UUID spaceId);
}
