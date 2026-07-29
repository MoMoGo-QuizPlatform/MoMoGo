package com.momogo.core.domain.report.dto.response;

import com.momogo.core.domain.report.entity.ReportType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 개인 리포트 응답 Dto
 * @param id 리포트 ID
 * @param reportType 리포트 종류 (DAILY | WEEKLY)
 * @param reportDate 집계 기준일 (일간이면 그 날짜, 주간이면 그 주의 시작일)
 * @param attemptedCount 시도한 문제 수 (맞았든 틀렸든 전체)
 * @param solvedCount 정답 문제 수 (그중 맞은 것만)
 * @param accuracyRate 정답률(%) — DB에는 저장하지 않고, attemptedCount/solvedCount로부터
 *                     매핑 시점(ReportMapper)에 계산해서 채워 넣는 값. 저장해두면 나중에
 *                     원본 두 숫자가 바뀌었을 때 값이 어긋날 수 있어서 항상 즉석 계산으로 처리함
 * @param createdAt 이 리포트가 배치에 의해 생성된 시각
 */
public record PersonalReportResponse(
    UUID id,
    ReportType reportType,
    LocalDate reportDate,
    Integer attemptedCount,
    Integer solvedCount,
    Double accuracyRate,
    OffsetDateTime createdAt
) {
}
