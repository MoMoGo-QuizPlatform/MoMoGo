package com.momogo.core.domain.problem.service;

import com.momogo.core.domain.problem.dto.request.ProblemAiCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemSolveRequest;
import com.momogo.core.domain.problem.dto.request.ProblemUpdateRequest;
import com.momogo.core.domain.problem.dto.response.ProblemCursorResponse;
import com.momogo.core.domain.problem.dto.response.ProblemDetailResponse;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.dto.response.ProblemSolveResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ProblemService {

  /**
   * 문제 직접 생성 (ADMIN 전용)
   * @param spaceId 공간 ID
   * @param request 문제 직접 생성 DTO
   */
  ProblemResponse createProblem(UUID spaceId, ProblemCreateRequest request);

  /**
   * 문제 목록 조회
   * @param spaceId         공간 ID
   * @param categoryId      카테고리 ID
   * @param nameKeyword     이름 검색용
   * @param contentKeyword  문제 내용 검색용
   * @param cursor          다음 커서 일자
   * @param cursorId        다음 커서 ID (서브)
   * @param size            조회하고자 하는 사이즈
   * @param userId          현재 로그인한 유저 ID (문제별 풀이 여부 표시용)
   */
  ProblemCursorResponse getProblems(
      UUID spaceId,
      UUID categoryId,
      String nameKeyword,
      String contentKeyword,
      OffsetDateTime cursor,
      UUID cursorId,
      int size,
      UUID userId
  );

  /**
   * 문제 단건 조회
   * @param problemId  문제 ID
   */
  ProblemDetailResponse getProblem(UUID spaceId, UUID problemId);

  /**
   * 문제 수정 (ADMIN 전용)
   * @param problemId 문제 ID
   * @param request   문제 수정 DTO
   */
  ProblemResponse updateProblem(UUID spaceId, UUID problemId, ProblemUpdateRequest request);

  /**
   * 문제 삭제 (ADMIN 전용)
   * @param problemId 문제 ID
   */
  void deleteProblem(UUID spaceId, UUID problemId);

  /**
   * 문제 제출 및 채점
   * @param spaceId   공간 ID
   * @param problemId 문제 ID
   * @param userId    유저 ID
   * @param request   문제 제출 요청 DTO
   */
  ProblemSolveResponse solveProblem(UUID spaceId, UUID problemId, UUID userId, ProblemSolveRequest request);

  /**
   * AI 기반 문제 자동 생성 (ADMIN 전용)
   * @param spaceId   공간 ID
   * @param request   AI 문제 자동 생성 요청 DTO
   */
  List<ProblemResponse> createProblemsByAi(UUID spaceId, ProblemAiCreateRequest request);
}
