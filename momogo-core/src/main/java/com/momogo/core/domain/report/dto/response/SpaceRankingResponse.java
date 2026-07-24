package com.momogo.core.domain.report.dto.response;

import java.util.UUID;

/**
 * 공간 랭킹 응답 Dto
 * @param userId 유저 ID
 * @param userName 유저 이름
 * @param profileImageUrl 유저 프로필 이미지 URL
 * @param solvedCount 정답 문제 수 (랭킹 기준)
 */
public record SpaceRankingResponse(
    UUID userId,
    String userName,
    String profileImageUrl,
    Long solvedCount
) {
}
