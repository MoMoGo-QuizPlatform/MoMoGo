package com.momogo.core.domain.space.repository;

import static com.momogo.core.domain.space.entity.QSpace.space;

import com.momogo.core.domain.space.entity.Space;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SpaceRepositoryImpl implements SpaceRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Space> findUnjoinedSpacesByCursor(
      UUID userSpaceId,
      UUID lastSpaceId,
      OffsetDateTime lastCreatedAt,
      int limit
  ) {
    return queryFactory
        .selectFrom(space)
        .where(
            userSpaceEq(userSpaceId), // 본인이 가입한 공간 제외
            cursorCondition(lastSpaceId, lastCreatedAt) // 커서 조건
        )
        .orderBy(space.createdAt.desc(), space.id.desc()) // 생성일 최신순, 동일할 시 ID 역순 정렬
        .limit(limit + 1) // hasNext 판단용 1개 더 조회
        .fetch();
  }

  // 본인 강비 공간 제외 동적 조건
  private BooleanExpression userSpaceEq(UUID userSpaceId) {
    return userSpaceId != null ? space.id.ne(userSpaceId) : null;
  }

  // 커서 필터링 동적 조건
  private BooleanExpression cursorCondition(UUID lastSpaceId, OffsetDateTime lastCreatedAt) {
    if (lastSpaceId == null || lastCreatedAt == null) {
      return null; // 첫 페이지 조회 시 조건 없음
    }
    return space.createdAt.lt(lastCreatedAt)
        .or(space.createdAt.eq(lastCreatedAt).and(space.id.lt(lastSpaceId)));
  }

}
