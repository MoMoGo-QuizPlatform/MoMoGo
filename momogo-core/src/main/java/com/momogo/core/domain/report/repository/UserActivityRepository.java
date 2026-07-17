package com.momogo.core.domain.report.repository;

import static com.momogo.core.domain.user.entity.QUserProblem.userProblem;

import com.momogo.core.domain.report.dto.UserActivityCount;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 배치 집계 전용 리포지토리. SpaceRankingRepository처럼 엔티티 없이 QueryDSL 집계 쿼리만 제공
@Repository
@RequiredArgsConstructor
public class UserActivityRepository {

  private final JPAQueryFactory queryFactory;

  /*
   * 특정 기간(start 이상 ~ end 미만) 동안 문제를 푼 모든 유저의 시도/정답 수를 유저별로 집계.
   *
   * - 유저를 한 명씩 돌면서 쿼리하면 유저 수만큼 쿼리가 나가는 N+1이 되므로,
   *   GROUP BY로 DB에서 한 번에 전부 집계해서 가져옴 (쿼리 1번으로 끝)
   * - 기간 내 활동이 아예 없는 유저는 결과에 안 나옴 -> 그런 유저는 리포트 행도 안 만들면 됨
   *   (조회 시 리포트가 없으면 ReportServiceImpl이 0짜리 응답을 만들어주므로 문제없음)
   * - solvedCount는 "시도 중 맞은 것만" 세야 해서, isSolved가 true면 1 아니면 0으로 바꿔서
   *   합산하는 CASE WHEN 방식(CaseBuilder)을 사용함
   */
  public List<UserActivityCount> findActivityCounts(OffsetDateTime start, OffsetDateTime end) {
    return queryFactory
        .select(Projections.constructor(UserActivityCount.class,
            userProblem.user.id,
            userProblem.count(),
            new CaseBuilder()
                .when(userProblem.isSolved.isTrue()).then(1L)
                .otherwise(0L)
                .sum()))
        .from(userProblem)
        .where(
            userProblem.createdAt.goe(start),
            userProblem.createdAt.lt(end)
        )
        .groupBy(userProblem.user.id)
        .fetch();
  }
}
