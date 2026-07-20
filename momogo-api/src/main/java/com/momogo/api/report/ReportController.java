package com.momogo.api.report;

import com.momogo.core.domain.report.dto.response.PersonalReportResponse;
import com.momogo.core.domain.report.dto.response.SpaceRankingResponse;
import com.momogo.core.domain.report.entity.ReportType;
import com.momogo.core.domain.report.service.ReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboards")
public class ReportController {

  private final ReportService reportService;

  /**
   * 개인 대시보드 리포트 조회 (마이페이지)
   *
   * @param userId 로그인한 유저 ID (토큰에서 추출)
   * @param reportType DAILY(일간) 또는 WEEKLY(주간)
   * @param reportDate 조회할 기준일. 생략하면 "존재하는 가장 최신 리포트"의 날짜로 조회
   *                   (배치는 항상 끝난 기간만 집계하므로 일간=어제, 주간=지난주.
   *                    오늘/이번주를 기본값으로 하면 아직 생성 전인 리포트를 찾아 항상 0이 나옴)
   * @return 해당 기준일의 시도/정답 수와 정답률 (리포트가 없으면 0으로 채워진 응답)
   */
  @GetMapping("/personal")
  public ResponseEntity<PersonalReportResponse> getPersonalReport(
      @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
      @RequestParam ReportType reportType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate
  ) {
    LocalDate targetDate = reportDate != null ? reportDate
        : (reportType == ReportType.DAILY
            ? LocalDate.now().minusDays(1)     // 어제 = 최신 일간 리포트
            : LocalDate.now().minusWeeks(1));  // 지난주 = 최신 주간 리포트 (서비스가 월요일로 보정)
    return ResponseEntity.ok(reportService.getPersonalReport(userId, reportType, targetDate));
  }

  /**
   * 공간 대시보드 랭킹 조회 (이번 주 정답 수 기준 내림차순)
   *
   * @param spaceId 랭킹을 조회할 공간 ID
   * @return 이번 주(월요일 00:00 이후) 정답 수 기준 유저 랭킹 목록
   */
  @GetMapping("/spaces/{spaceId}/ranking")
  public ResponseEntity<List<SpaceRankingResponse>> getSpaceRanking(
      @PathVariable UUID spaceId
  ) {
    return ResponseEntity.ok(reportService.getSpaceRanking(spaceId));
  }
}
