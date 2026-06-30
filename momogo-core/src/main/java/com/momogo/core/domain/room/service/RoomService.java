package com.momogo.core.domain.room.service;

import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import java.util.UUID;

public interface RoomService {

  RoomResponse createRoom(UUID userId, UUID spaceId, RoomCreateRequest request);
}
