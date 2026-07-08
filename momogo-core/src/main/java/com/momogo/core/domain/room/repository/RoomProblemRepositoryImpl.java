package com.momogo.core.domain.room.repository;

import static com.momogo.core.domain.room.entity.QRoomProblem.roomProblem;

import com.momogo.core.domain.room.entity.RoomProblem;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
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
  public void decreaseOrderAfter(UUID roomId, int targetOrder) {
    queryFactory
        .update(roomProblem)
        .set(roomProblem.problemOrder, roomProblem.problemOrder.subtract(1))
        .where(
            roomProblem.room.id.eq(roomId),
            roomProblem.problemOrder.gt(targetOrder)
        )
        .execute();
  }

  // 방의 문제 중 가장 큰 problemOrder 조회 (AI 생성 시 채번용)
  @Override
  public Optional<Integer> findMaxProblemOrderByRoomId(UUID roomId) {
    return Optional.ofNullable(
        queryFactory
            .select(roomProblem.problemOrder.max())
            .from(roomProblem)
            .where(roomProblem.room.id.eq(roomId))
            .fetchOne()
    );
  }
}
