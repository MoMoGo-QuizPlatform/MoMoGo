package com.momogo.batch.scheduler;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * 배치 Job을 정해진 시간에 실행시키는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

  private final JobLauncher jobLauncher;
  private final Job dailyReportJob;
  private final Job weeklyReportJob;

  // 매일 00:10 - 어제 하루치 개인 일간 리포트 생성
  @Scheduled(cron = "0 10 0 * * *")
  public void runDailyReportJob() {
    try {
      jobLauncher.run(dailyReportJob, new JobParametersBuilder()
          .addString("runDate", LocalDate.now().toString())
          .toJobParameters());
    } catch (Exception e) {
      // 스케줄러 안에서 예외가 새어나가면 이후 스케줄 동작에 영향을 줄 수 있으므로 로그만 남김
      log.error("일간 리포트 배치 실행 실패", e);
    }
  }

  // 매주 월요일 00:20 - 지난주 개인 주간 리포트 생성
  @Scheduled(cron = "0 20 0 * * MON")
  public void runWeeklyReportJob() {
    try {
      jobLauncher.run(weeklyReportJob, new JobParametersBuilder()
          .addString("runDate", LocalDate.now().toString())
          .toJobParameters());
    } catch (Exception e) {
      log.error("주간 리포트 배치 실행 실패", e);
    }
  }
}
