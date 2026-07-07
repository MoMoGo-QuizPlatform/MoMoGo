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

  /**
   * targetOrder보다 큰 순번들을 일괄 -1 (삭제 후 순전 재정렬 -> 벌크 연산)
   * @param roomId      방의 ID
   * @param targetOrder 삭제된 문제의 순전
   * @return            영향받은 row 수
   */
  long decreaseOrderAfter(UUID roomId, int targetOrder);
}
