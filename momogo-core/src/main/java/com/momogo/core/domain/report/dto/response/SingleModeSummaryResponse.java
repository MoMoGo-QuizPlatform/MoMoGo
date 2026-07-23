package com.momogo.core.domain.report.dto.response;

/**
 * 공간 내 "성적 대시보드" 탭의 싱글모드 요약 카드 응답 Dto
 * @param dailySolvedCount 오늘 정답 처리된 문제 수
 * @param weeklySolvedCount 이번 주(월요일~현재) 정답 처리된 문제 수
 * @param averageCorrectRate 전체 누적 평균 정답률(%)
 */
public record SingleModeSummaryResponse(
    long dailySolvedCount,
    long weeklySolvedCount,
    double averageCorrectRate
) {
}
