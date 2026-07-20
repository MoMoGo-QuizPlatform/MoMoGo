package com.momogo.core.domain.report.repository;

import com.momogo.core.domain.report.entity.PersonalReport;
import com.momogo.core.domain.report.entity.ReportType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonalReportRepository extends JpaRepository<PersonalReport, UUID> {

  // 특정 유저의 특정 기준일 리포트 조회 (일간/주간 공용)
  Optional<PersonalReport> findByUserIdAndReportTypeAndReportDate(
      UUID userId, ReportType reportType, LocalDate reportDate);

  // 특정 기준일에 이미 리포트가 있는 유저 ID만 조회 (배치 재실행 시 중복 생성 방지용)
  // 엔티티 전체를 로드하지 않고 ID만 뽑아 메모리 사용을 줄임
  @Query("SELECT p.user.id FROM PersonalReport p "
      + "WHERE p.reportType = :reportType AND p.reportDate = :reportDate")
  List<UUID> findUserIdsByReportTypeAndReportDate(
      @Param("reportType") ReportType reportType, @Param("reportDate") LocalDate reportDate);
}
