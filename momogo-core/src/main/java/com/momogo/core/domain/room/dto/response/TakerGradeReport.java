package com.momogo.core.domain.room.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 응시자별 종합 성적 리포트 DTO
 * @param userId 응시자 아이디
 * @param name 응시자 이름
 * @param email 응시자 이메일
 * @param profileImageUrl 응시자 프로필 이미지 URL
 * @param isAttended 응시 여부
 * @param score 응시 점수
 * @param problemGrades 문항별 제출 답안 및 채점 여부 목록
 */
public record TakerGradeReport(
    UUID userId,
    String name,
    String email,
    String profileImageUrl,
    boolean isAttended,
    int score,
    List<ProblemGradeReport> problemGrades
) {

}
