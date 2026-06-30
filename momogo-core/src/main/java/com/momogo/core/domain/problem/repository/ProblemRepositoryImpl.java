package com.momogo.core.domain.problem.repository;

import static com.momogo.core.domain.problem.entity.QProblem.problem;
import static com.momogo.core.domain.problem.entity.QProblemCategory.problemCategory;

import com.momogo.core.domain.problem.entity.Problem;
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

  @Override
  public List<Problem> findWithCursor(
      UUID spaceId,
      UUID categoryId,
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
            categoryIdCondition(categoryId),
            nameKeywordCondition(nameKeyword),
            contentKeywordCondition(contentKeyword),
            cursorCondition(cursor, cursorId)
        )
        .orderBy(problem.createdAt.desc(), problem.id.desc())
        .limit(size)
        .fetch();
  }

  // 공통 조건 메서드 //

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

  // 카테고리 조건
  private BooleanExpression categoryIdCondition(UUID categoryId) {
    return categoryId != null ? problem.category.id.eq(categoryId) : null;
  }
}
