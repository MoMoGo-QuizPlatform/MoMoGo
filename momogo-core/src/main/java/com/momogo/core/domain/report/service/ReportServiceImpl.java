package com.momogo.core.domain.report.service;

import com.momogo.core.domain.report.dto.response.PersonalReportResponse;
import com.momogo.core.domain.report.dto.response.SpaceRankingResponse;
import com.momogo.core.domain.report.entity.ReportType;
import com.momogo.core.domain.report.mapper.ReportMapper;
import com.momogo.core.domain.report.repository.PersonalReportRepository;
import com.momogo.core.domain.report.repository.SpaceRankingRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

  private final PersonalReportRepository personalReportRepository;
  private final SpaceRankingRepository spaceRankingRepository;
  private final ReportMapper reportMapper;

  @Override
  public PersonalReportResponse getPersonalReport(UUID userId, ReportType reportType, LocalDate reportDate) {
    LocalDate targetDate = reportType == ReportType.WEEKLY
        ? reportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        : reportDate;

    // 배치가 아직 안 돌았거나(오늘 첫 문제 풀이 전 등) 그날 활동이 없어서 리포트 행 자체가
    // 없을 수 있음. 이 경우 404를 던지는 대신, "활동 없음"을 뜻하는 0짜리 응답을 만들어 반환함
    // (대시보드 입장에서는 에러가 아니라 그냥 텅 빈 하루/주로 보여주는 게 자연스러움)
    return personalReportRepository.findByUserIdAndReportTypeAndReportDate(userId, reportType, targetDate)
        .map(reportMapper::toResponse)
        .orElseGet(() -> new PersonalReportResponse(null, reportType, targetDate, 0, 0, 0.0, null));
  }

  @Override
  public List<SpaceRankingResponse> getSpaceRanking(UUID spaceId) {
    // "이번 주"의 시작 시각(월요일 00:00)을 구해서, 그 이후에 풀린 문제만 랭킹에 반영함
    // (전체 누적이 아니라 "이번 주" 랭킹이라는 요구사항이라, 매주 월요일이 되면 자연스럽게 초기화됨)
    LocalDate mondayOfThisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    OffsetDateTime periodStart = mondayOfThisWeek.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

    return spaceRankingRepository.findRankingBySpaceId(spaceId, periodStart);
  }
}
