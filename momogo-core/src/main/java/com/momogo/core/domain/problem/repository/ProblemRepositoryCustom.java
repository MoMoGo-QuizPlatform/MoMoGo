package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.Problem;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ProblemRepositoryCustom {

  /**
   * 공간 내 전체 문제 목록
   */
  List<Problem> findWithCursor(
      UUID spaceId,
      UUID categoryId,
      String nameKeyword,
      String contentKeyword,
      OffsetDateTime cursor,
      UUID cursorId,
      int size
  );

}
