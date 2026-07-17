package com.momogo.core.domain.report.dto;

import java.util.UUID;

/**
 * 배치 집계용 내부 DTO — 특정 기간 동안 한 유저가 얼마나 활동했는지 집계한 결과.
 * 클라이언트에 나가는 응답이 아니라, 배치 Job이 리포트를 만들 때 중간 재료로만 쓰는 값
 *
 * @param userId 유저 ID
 * @param attemptedCount 기간 내 시도한 문제 수 (맞았든 틀렸든 전체)
 * @param solvedCount 기간 내 정답 문제 수 (그중 맞은 것만)
 */
public record UserActivityCount(
    UUID userId,
    Long attemptedCount,
    Long solvedCount
) {
}
