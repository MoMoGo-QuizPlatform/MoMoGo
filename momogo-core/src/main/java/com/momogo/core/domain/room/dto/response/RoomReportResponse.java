package com.momogo.core.domain.room.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * 시험방 최종 성적 리포트 DTO
 * @param roomId 시험방 아이디
 * @param roomName 시험방 이름
 * @param totalApplicants 총 응시자 수
 * @param attendedCount 실제 응시자 수
 * @param averageScore 평균 점수
 * @param maxScore 최고 점수
 * @param takerGrades 응시자별 성적 목록
 */
public record RoomReportResponse(
    UUID roomId,
    String roomName,
    Integer totalApplicants,
    Integer attendedCount,
    Double averageScore,
    Integer maxScore,
    List<TakerGradeReport> takerGrades
) {

}
