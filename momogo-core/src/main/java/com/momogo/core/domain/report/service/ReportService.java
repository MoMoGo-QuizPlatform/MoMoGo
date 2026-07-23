package com.momogo.core.domain.report.service;

import com.momogo.core.domain.report.dto.response.DashboardSummaryResponse;
import com.momogo.core.domain.report.dto.response.MyExamDetailResponse;
import com.momogo.core.domain.report.dto.response.MyExamListItemResponse;
import com.momogo.core.domain.report.dto.response.PersonalReportResponse;
import com.momogo.core.domain.report.dto.response.SingleModeHistoryResponse;
import com.momogo.core.domain.report.dto.response.SingleModeSummaryResponse;
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

  // 메인 대시보드 요약 카드(오늘 푼 문제 수 / 이번 주 정답률 / 참여 완료 시험 수) 실시간 조회
  DashboardSummaryResponse getDashboardSummary(UUID userId);

  // 공간 내 성적 대시보드: 싱글모드 요약(오늘/이번 주 정답 수, 전체 평균 정답률) 조회
  SingleModeSummaryResponse getSingleModeSummary(UUID userId);

  // 공간 내 성적 대시보드: 싱글모드 전체 풀이 이력 조회
  List<SingleModeHistoryResponse> getSingleModeHistory(UUID userId);

  // 공간 내 성적 대시보드: 내가 응시(제출 완료)한 평가 시험 목록 조회
  List<MyExamListItemResponse> getMyExamList(UUID userId);

  // 공간 내 성적 대시보드: 내가 응시한 평가 시험의 문항별 채점 결과(리뷰) 조회
  MyExamDetailResponse getMyExamDetail(UUID userId, UUID roomId);
}
