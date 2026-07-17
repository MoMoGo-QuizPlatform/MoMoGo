package com.momogo.batch.job;

import com.momogo.core.domain.report.dto.UserActivityCount;
import com.momogo.core.domain.report.entity.PersonalReport;
import com.momogo.core.domain.report.entity.ReportType;
import com.momogo.core.domain.report.repository.PersonalReportRepository;
import com.momogo.core.domain.report.repository.UserActivityRepository;
import com.momogo.core.domain.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class PersonalReportBatchJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final UserActivityRepository userActivityRepository;
  private final PersonalReportRepository personalReportRepository;
  private final UserRepository userRepository;

  //일간 리포트 Job
  @Bean
  public Job dailyReportJob() {
    return new JobBuilder("dailyReportJob", jobRepository)
        .start(dailyReportStep())
        .build();
  }

  @Bean
  public Step dailyReportStep() {
    return new StepBuilder("dailyReportStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          // 매일 자정 직후에 실행되므로, 집계 대상은 어제 하루 (어제 00:00 이상 ~ 오늘 00:00 미만)
          LocalDate yesterday = LocalDate.now().minusDays(1);
          createReports(ReportType.DAILY, yesterday, yesterday, yesterday.plusDays(1));
          return RepeatStatus.FINISHED; // 이 Tasklet은 한 번 실행으로 끝이라는 뜻
        }, transactionManager)
        .build();
  }

  //주간 리포트 Job
  @Bean
  public Job weeklyReportJob() {
    return new JobBuilder("weeklyReportJob", jobRepository)
        .start(weeklyReportStep())
        .build();
  }

  @Bean
  public Step weeklyReportStep() {
    return new StepBuilder("weeklyReportStep", jobRepository)
        .tasklet((contribution, chunkContext) -> {
          // 매주 월요일 자정 직후에 실행되므로, 집계 대상은 "지난주" (지난주 월요일 ~ 이번주 월요일 미만)
          // reportDate(기준일)는 지난주 월요일로 저장 -> 조회할 때도 "그 주의 월요일"로 찾으면 됨
          LocalDate lastMonday = LocalDate.now().minusWeeks(1);
          createReports(ReportType.WEEKLY, lastMonday, lastMonday, lastMonday.plusWeeks(1));
          return RepeatStatus.FINISHED;
        }, transactionManager)
        .build();
  }

  //공통 리포트 생성 로직

  /*
   * 기간(periodStart ~ periodEnd) 동안의 유저별 활동을 집계해서 PersonalReport로 저장.
   *
   * @param reportType  DAILY 또는 WEEKLY
   * @param reportDate  리포트의 기준일로 저장할 날짜 (일간=그날, 주간=그 주 월요일)
   * @param periodStart 집계 시작일 (이상)
   * @param periodEnd   집계 종료일 (미만)
   */
  private void createReports(ReportType reportType, LocalDate reportDate,
                             LocalDate periodStart, LocalDate periodEnd) {

    //LocalDate(날짜) -> OffsetDateTime(시각) 변환. created_at 컬럼이 TIMESTAMPTZ라서
    //그날 00:00 (서버 시간대 기준)으로 맞춰서 비교해야 함
    ZoneId zone = ZoneId.systemDefault();
    OffsetDateTime start = periodStart.atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime end = periodEnd.atStartOfDay(zone).toOffsetDateTime();

    // 기간 내 활동한 모든 유저의 시도/정답 수를 쿼리 1번으로 집계
    List<UserActivityCount> counts = userActivityRepository.findActivityCounts(start, end);

    //같은 기준일 리포트가 이미 있으면 건너뛰기 위한 준비 (배치가 실수로 두 번 돌아도 중복 생성 방지)
    //유저별로 매번 존재 여부를 쿼리하면 N+1이 되므로, 이미 있는 유저 ID들을 한 번에 가져와 Set으로 만듦
    Set<UUID> alreadyReported = personalReportRepository
        .findAllByReportTypeAndReportDate(reportType, reportDate).stream()
        .map(report -> report.getUser().getId())
        .collect(Collectors.toSet());

    //집계 결과를 리포트 엔티티로 변환해서 저장
    List<PersonalReport> reports = counts.stream()
        .filter(count -> !alreadyReported.contains(count.userId()))
        .map(count -> PersonalReport.builder()
            // getReferenceById: 유저를 실제로 SELECT하지 않고 FK 저장용 프록시만 얻는 방법.
            // 여기서는 리포트에 user_id만 채우면 되므로 유저 전체 조회가 불필요함 (쿼리 절약)
            .user(userRepository.getReferenceById(count.userId()))
            .reportType(reportType)
            .reportDate(reportDate)
            .attemptedCount(count.attemptedCount().intValue())
            .solvedCount(count.solvedCount().intValue())
            .build())
        .toList();

    personalReportRepository.saveAll(reports);
    log.info("[{}] 리포트 생성 완료 - 기준일: {}, 생성: {}건 (중복 스킵: {}건)",
        reportType, reportDate, reports.size(), counts.size() - reports.size());
  }
}
