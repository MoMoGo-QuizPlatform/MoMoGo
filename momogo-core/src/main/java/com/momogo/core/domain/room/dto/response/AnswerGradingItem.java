package com.momogo.core.domain.room.dto.response;

import java.util.UUID;

/**
 * 채점 검토 화면의 답안 한 건 DTO
 * @param answerId 답안 아이디
 * @param userId 응시자 아이디
 * @param userName 응시자 이름
 * @param userProfileImageUrl 응시자 프로필 이미지 URL
 * @param problemId 문제 아이디
 * @param problemOrder 문제 순서
 * @param problemName 문제명
 * @param userAnswer 응시자 제출 답안
 * @param correctAnswer 모범 정답
 * @param isCorrect 현재 정답 여부 (아직 채점 전이면 null)
 */
public record AnswerGradingItem(
    UUID answerId,
    UUID userId,
    String userName,
    String userProfileImageUrl,
    UUID problemId,
    Integer problemOrder,
    String problemName,
    String userAnswer,
    String correctAnswer,
    Boolean isCorrect
) {

}
