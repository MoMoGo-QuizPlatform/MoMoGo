package com.momogo.core.domain.room.service;

import com.momogo.core.domain.room.dto.request.RoomProblemCreatedRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemUpdateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemExamResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import java.util.List;
import java.util.UUID;

public interface RoomProblemService {

  /**
   * 방 문제 추가 (ADMIN 전용)
   * @param userId  유저 ID
   * @param roomId  방 ID
   * @param request 방 문제 생성 DTO
   */
  RoomProblemResponse createRoomProblem(UUID userId, UUID roomId, RoomProblemCreatedRequest request);

  /**
   * 방 문제 목록 조회 (ADMIN 전용, 시험 시작 여부 무관 항상 조회 가능)
   * @param userId   유저 ID
   * @param roomId   방 ID
   */
  List<RoomProblemResponse> getRoomProblemsForAdmin(UUID userId, UUID roomId);

  /**
   * 방 문제 목록 조회 (응시자 전용, 시험 시작 전엔 조회 불가, 정답/해설 안 보임)
   * @param userId  유저 ID
   * @param roomId  방 ID
   */
  List<RoomProblemExamResponse> getRoomProblemsForExam(UUID userId, UUID roomId);

  /**
   * 방 문제 수정 (ADMIN 전용)
   * @param userId        유저 ID
   * @param roomId        방 ID
   * @param roomProblemId 방 문제 ID
   * @param request       방 문제 수정 DTO
   */
  RoomProblemResponse updateRoomProblem(UUID userId, UUID roomId, UUID roomProblemId, RoomProblemUpdateRequest request);

  /**
   * 방 문제 삭제 (ADMIN 전용) - 삭제 후 순번 재정렬
   * @param userId        유저 ID
   * @param roomId        방 ID
   * @param roomProblemId 방 문제 ID
   */
  void deleteRoomProblem(UUID userId, UUID roomId, UUID roomProblemId);
}
