package com.momogo.core.domain.report.repository;

import static com.momogo.core.domain.user.entity.QUser.user;
import static com.momogo.core.domain.user.entity.QUserProblem.userProblem;

import com.momogo.core.domain.report.dto.response.SpaceRankingResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 엔티티 없이 QueryDSL 집계 쿼리만 제공하는 레포지토리
@Repository
@RequiredArgsConstructor
public class SpaceRankingRepository {

  private final JPAQueryFactory queryFactory;

  // 공간 내 유저별 정답 문제 수 랭킹 (periodStart 이후 데이터만 집계, 정답 수 내림차순)
  public List<SpaceRankingResponse> findRankingBySpaceId(UUID spaceId, OffsetDateTime periodStart) {
    return queryFactory
        .select(Projections.constructor(SpaceRankingResponse.class,
            user.id, user.name, user.profileImageUrl, userProblem.count()))
        .from(userProblem)
        .join(userProblem.user, user)
        .where(
            user.space.id.eq(spaceId),
            userProblem.isSolved.isTrue(),
            userProblem.createdAt.goe(periodStart)
        )
        .groupBy(user.id, user.name, user.profileImageUrl)
        .orderBy(userProblem.count().desc())
        .fetch();
  }
}
