package com.momogo.core.domain.room.service;

import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemDraftAiRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.dto.response.RoomReportResponse;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface RoomService {

  RoomResponse createRoom(UUID userId, UUID spaceId, RoomCreateRequest request);

  // 시험방 생성 마법사(2단계) 미리보기용 AI 문제 생성 - DB에 저장하지 않고 생성 결과만 반환
  List<GeneratedProblemData> generateDraftProblems(UUID userId, UUID spaceId, RoomProblemDraftAiRequest request);

  RoomResponse getRoomDetails(UUID userId, UUID roomId);

  List<RoomProblemResponse> getRoomProblems(UUID userId, UUID roomId);

  void submitRoomAnswer(UUID userId, UUID roomId, RoomAnswerSubmitRequest request);

  void finalizeGrade(UUID adminUserId, UUID roomId);

  RoomReportResponse getRoomReport(UUID adminUserId, UUID roomId);

  byte[] downloadRoomReportPdf(UUID adminUserId, UUID roomId);

  void startAiGrading(UUID adminUserId, UUID roomId);

  void saveGradingResults(Map<UUID, Boolean> gradingResults, UUID roomId);

  void clearAiGradingStatus(UUID roomId);
}
