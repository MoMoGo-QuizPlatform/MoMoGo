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
}
