package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.RoomProblem;
import java.util.List;
import java.util.UUID;

public interface RoomProblemRepositoryCustom {

  /**
   * 방의 문제 목록을 출제 순으로 조회
   * @param roomId  방의 ID
   */
  List<RoomProblem> findByRoomIdOrderByProblemOrder(UUID roomId);
}
