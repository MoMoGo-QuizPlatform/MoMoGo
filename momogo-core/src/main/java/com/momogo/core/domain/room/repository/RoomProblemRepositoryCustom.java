package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.RoomProblem;
import java.util.List;
import java.util.Optional;
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
   */
  void decreaseOrderAfter(UUID roomId, int targetOrder);

  /**
   * 방의 문제 중 가장 큰 problemOrder 조회 (AI 생성 시 채번용)
   * @param roomId  방의 ID
   */
  Optional<Integer> findMaxProblemOrderByRoomId(UUID roomId);
}
