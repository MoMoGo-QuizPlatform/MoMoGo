package com.momogo.core.domain.room.repository;

import static com.momogo.core.domain.room.entity.QRoomProblem.roomProblem;

import com.momogo.core.domain.room.entity.RoomProblem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoomProblemRepositoryImpl implements RoomProblemRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 방 문제 목록 (출제 순서 - 오름차순)
  @Override
  public List<RoomProblem> findByRoomIdOrderByProblemOrder(UUID roomId) {

    return queryFactory
        .selectFrom(roomProblem)
        .where(roomProblem.room.id.eq(roomId))
        .orderBy(roomProblem.problemOrder.asc())
        .fetch();
  }

  // 삭제된 문제보다 큰 순번들을 한 번에 -1 시키는 벌크 연산
  @Override
  public long decreaseOrderAfter(UUID roomId, int targetOrder) {
    return queryFactory
        .update(roomProblem)
        .set(roomProblem.problemOrder, roomProblem.problemOrder.subtract(1))
        .where(
            roomProblem.room.id.eq(roomId),
            roomProblem.problemOrder.gt(targetOrder)
        )
        .execute();
  }
}
