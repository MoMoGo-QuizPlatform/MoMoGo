package com.momogo.core.domain.problem.repository.impl;

import static com.momogo.core.domain.problem.entity.QProblem.problem;
import static com.momogo.core.domain.problem.entity.QProblemCategory.problemCategory;

import com.momogo.core.domain.problem.entity.Problem;
import com.momogo.core.domain.problem.repository.ProblemRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProblemRepositoryImpl implements ProblemRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 공간 내 전체 문제 목록
  @Override
  public List<Problem> findAllWithCursor(
      UUID spaceId,
      OffsetDateTime cursor,
      UUID cursorId,
      int size) {

    return queryFactory
        .selectFrom(problem)
        .join(problem.category, problemCategory).fetchJoin()
        .where(
            problem.space.id.eq(spaceId),
            cursorCondition(cursor, cursorId)
        )
        .orderBy(
            problem.createdAt.desc(),
            problem.id.desc()
        )
        .limit(size)
        .fetch();
  }

  // 공간 내 카테고리별 문제 목록
  @Override
  public List<Problem> findByCategoryWithCursor(
      UUID spaceId,
      UUID categoryId,
      OffsetDateTime cursor,
      UUID cursorId,
      int size) {

    return queryFactory
        .selectFrom(problem)
        .join(problem.category, problemCategory).fetchJoin()
        .where(
            problem.space.id.eq(spaceId),
            problem.category.id.eq(categoryId),
            cursorCondition(cursor, cursorId)
        )
        .orderBy(
            problem.createdAt.desc(),
            problem.id.desc()
        )
        .limit(size)
        .fetch();
  }

  // 공간 내 이름 + 내용 키워드 검색 목록
  @Override
  public List<Problem> searchByKeywordWithCursor(
      UUID spaceId,
      String nameKeyword,
      String contentKeyword,
      OffsetDateTime cursor,
      UUID cursorId,
      int size) {

    return queryFactory
        .selectFrom(problem)
        .join(problem.category, problemCategory).fetchJoin()
        .where(
            problem.space.id.eq(spaceId),
            nameKeywordCondition(nameKeyword),       // null이면 자동 제외
            contentKeywordCondition(contentKeyword), // null이면 자동 제외
            cursorCondition(cursor, cursorId)
        )
        .orderBy(
            problem.createdAt.desc(),
            problem.id.desc()
        )
        .limit(size)
        .fetch();
  }

  // 공통 조건 메서드

  // 커서 조건
  private BooleanExpression cursorCondition(OffsetDateTime cursor, UUID cursorId) {

    if (cursor == null || cursorId == null) {
      return null;
    }

    return problem.createdAt.lt(cursor)
        .or(problem.createdAt.eq(cursor)
            .and(problem.id.lt(cursorId)));
  }

  // 이름 키워드 조건
  private BooleanExpression nameKeywordCondition(String keyword) {

    if (keyword == null || keyword.isBlank()) {
      return null;
    }

    return problem.name.containsIgnoreCase(keyword);
  }

  // 내용 키워드 조건
  private BooleanExpression contentKeywordCondition(String keyword) {

    if (keyword == null || keyword.isBlank()) {
      return null;
    }

    return problem.content.containsIgnoreCase(keyword);
  }
}
