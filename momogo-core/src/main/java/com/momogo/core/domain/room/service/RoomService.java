package com.momogo.core.domain.room.service;

import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import java.util.List;
import java.util.UUID;

public interface RoomService {

  RoomResponse createRoom(UUID userId, UUID spaceId, RoomCreateRequest request);

  RoomResponse getRoomDetails(UUID userId, UUID roomId);

  List<RoomProblemResponse> getRoomProblems(UUID userId, UUID roomId);

  void submitRoomAnswer(UUID userId, UUID roomId, RoomAnswerSubmitRequest request);
}
