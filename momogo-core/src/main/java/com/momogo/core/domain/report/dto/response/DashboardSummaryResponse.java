package com.momogo.core.domain.report.dto.response;

/**
 * 메인 대시보드 요약 카드 응답 Dto (싱글모드/종합성과/참여시험)
 * @param todaySolvedCount 오늘 정답 처리된 문제 수 (싱글모드)
 * @param weeklyAccuracyRate 이번 주(월요일~현재) 실시간 평균 정답률(%)
 * @param completedExamCount 응시(제출) 완료한 평가 시험방 수
 */
public record DashboardSummaryResponse(
    long todaySolvedCount,
    double weeklyAccuracyRate,
    long completedExamCount
) {
}
