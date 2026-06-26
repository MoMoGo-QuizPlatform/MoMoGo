package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.Problem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ProblemRepositoryCustom {

  /**
   * 공간 내 전체 문제 목록 조회
   *
   * @param spaceId  조회할 공간 ID
   * @param cursor   마지막으로 받은 createdAt
   * @param cursorId 마지막으로 받은 problem ID
   * @param size     한 번에 가져올 개수
   */
  List<Problem> findAllWithCursor(
      UUID spaceId,
      OffsetDateTime cursor,
      UUID cursorId,
      int size
  );

  /**
   * 공간 내 카테고리별 문제 목록 조회
   * @param spaceId    조회할 공간 ID
   * @param categoryId 조회할 카테고리 ID
   * @param cursor     마지막으로 받은 createdAt
   * @param cursorId   마지막으로 받은 problem ID
   * @param size       한 번에 가져올 개수
   */
  List<Problem> findByCategoryWithCursor(
      UUID spaceId,
      UUID categoryId,
      OffsetDateTime cursor,
      UUID cursorId,
      int size
  );

  /**
   * 공간 내 이름 + 내용 키워드 검색 목록 조회
   * 이름, 내용 검색은 각각 독립 적용
   * 예시
   * - 이름만 검색: searchByKeywordWithCursor(spaceId, "피타고라스", null, ...)
   * - 내용만 검색: searchByKeywordWithCursor(spaceId, null, "삼각형", ...)
   * - 둘 다 검색:  searchByKeywordWithCursor(spaceId, "피타고라스", "삼각형", ...)
   *
   * @param spaceId        조회할 공간 ID
   * @param nameKeyword    문제 이름 검색어
   * @param contentKeyword 문제 내용 검색어
   * @param cursor         마지막으로 받은 createdAt
   * @param cursorId       마지막으로 받은 problem ID
   * @param size           한 번에 가져올 개수
   */
  List<Problem> searchByKeywordWithCursor(
      UUID spaceId,
      String nameKeyword,
      String contentKeyword,
      OffsetDateTime cursor,
      UUID cursorId,
      int size
  );

}
