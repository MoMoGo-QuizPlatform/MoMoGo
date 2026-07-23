package com.momogo.core.domain.problem.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.request.ProblemAiCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemCreateRequest;
import com.momogo.core.domain.problem.dto.request.ProblemSolveRequest;
import com.momogo.core.domain.problem.dto.request.ProblemUpdateRequest;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.dto.response.ProblemCursorResponse;
import com.momogo.core.domain.problem.dto.response.ProblemDetailResponse;
import com.momogo.core.domain.problem.dto.response.ProblemResponse;
import com.momogo.core.domain.problem.dto.response.ProblemSolveResponse;
import com.momogo.core.domain.problem.entity.Problem;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.problem.entity.ProblemCounters;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.mapper.ProblemMapper;
import com.momogo.core.domain.problem.repository.ProblemCategoryRepository;
import com.momogo.core.domain.problem.repository.ProblemCountersRepository;
import com.momogo.core.domain.problem.repository.ProblemRepository;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.UserProblem;
import com.momogo.core.domain.user.entity.UserProblemId;
import com.momogo.core.domain.user.repository.UserProblemRepository;
import com.momogo.core.domain.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemServiceImpl implements ProblemService {

  private final ProblemRepository problemRepository;

  private final ProblemCategoryRepository categoryRepository;

  private final ProblemCountersRepository countersRepository;

  private final SpaceRepository spaceRepository;

  private final ProblemMapper problemMapper;
  
  private final ProblemGenerationService problemGenerationService;

  private final ProblemPersister problemPersister;

  private final UserProblemRepository userProblemRepository;

  private final UserRepository userRepository;

  /**
   * 문제 직접 생성
   *
   * @param spaceId 공간 ID
   * @param request 문제 직접 생성 DTO
   */
  @Override
  @Transactional
  public ProblemResponse createProblem(UUID spaceId, ProblemCreateRequest request) {

    // 공간 존재 여부 확인
    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.SPACE_NOT_FOUND));

    // 카테고리 존재 여부 확인
    ProblemCategory category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.CATEGORY_NOT_FOUND));

    Problem savedProblem = problemRepository.save(
        Problem.create(
            category,
            space,
            request.name(),
            request.content(),
            request.explanation(),
            request.correctAnswer()
        )
    );

    // 문제 통계 초기화
    countersRepository.save(ProblemCounters.init(savedProblem));

    return problemMapper.toResponse(savedProblem);
  }

  /**
   * 문제 목록 조회 (커서 페이지네이션)
   *
   * @param spaceId        공간 ID
   * @param categoryId     카테고리 ID
   * @param nameKeyword    이름 검색용
   * @param contentKeyword 문제 내용 검색용
   * @param cursor         다음 커서 일자
   * @param cursorId       다음 커서 ID (서브)
   */
  @Override
  public ProblemCursorResponse getProblems(
      UUID spaceId,
      UUID categoryId,
      String nameKeyword,
      String contentKeyword,
      OffsetDateTime cursor,
      UUID cursorId,
      int size) {

    if ((cursor == null) != (cursorId == null)) {
      throw new BusinessException(ProblemErrorCode.INVALID_CURSOR);
    }

    int fetchSize = size + 1;

    List<Problem> problemList = problemRepository.findWithCursor(
        spaceId, categoryId, nameKeyword, contentKeyword, cursor, cursorId, fetchSize);

    boolean hasNext = problemList.size() > size;

    List<Problem> content = hasNext ? problemList.subList(0, size) : problemList;

    Problem last = hasNext ? content.getLast() : null;

    return new ProblemCursorResponse(
        problemMapper.toResponseList(content),
        hasNext,
        last != null ? last.getCreatedAt() : null,
        last != null ? last.getId() : null
    );
  }

  /**
   * 문제 단건 조회
   *
   * @param problemId 문제 ID
   */
  @Override
  public ProblemDetailResponse getProblem(UUID spaceId, UUID problemId) {

    Problem problem = problemRepository.findByIdWithCategory(problemId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.PROBLEM_NOT_FOUND));

    if (!problem.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_NOT_IN_SPACE);
    }

    return problemMapper.toDetailResponse(problem);
  }

  /**
   * 문제 수정
   *
   * @param problemId 문제 ID
   * @param request   문제 수정 DTO
   */
  @Override
  @Transactional
  public ProblemResponse updateProblem(UUID spaceId, UUID problemId, ProblemUpdateRequest request) {

    Problem problem = problemRepository.findByIdWithCategory(problemId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.PROBLEM_NOT_FOUND));

    if (!problem.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_NOT_IN_SPACE);
    }

    if (request.name() != null && request.name().isBlank()) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_FIELD_BLANK);
    }
    if (request.content() != null && request.content().isBlank()) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_FIELD_BLANK);
    }
    if (request.correctAnswer() != null && request.correctAnswer().isBlank()) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_FIELD_BLANK);
    }

    problem.update(
        request.name(),
        request.content(),
        request.explanation(),
        request.correctAnswer()
    );

    return problemMapper.toResponse(problem);
  }

  /**
   * 문제 삭제
   *
   * @param problemId 문제 ID
   */
  @Override
  @Transactional
  public void deleteProblem(UUID spaceId, UUID problemId) {

    Problem problem = problemRepository.findById(problemId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.PROBLEM_NOT_FOUND));

    if (!problem.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_NOT_IN_SPACE);
    }

    // 카운터 먼저 제거
    countersRepository.deleteById(problemId);

    // 문제 제거
    problemRepository.deleteById(problemId);
  }

  /**
   * 문제 제출 (채점)
   *
   * @param problemId 문제 ID
   * @param userId    유저 ID
   * @param request   문제 제출 요청 DTO
   */
  @Override
  @Transactional
  public ProblemSolveResponse solveProblem(
      UUID spaceId,
      UUID problemId,
      UUID userId,
      ProblemSolveRequest request) {

    Problem problem = problemRepository.findById(problemId)
        .orElseThrow(() -> new BusinessException(ProblemErrorCode.PROBLEM_NOT_FOUND));

    if (!problem.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(ProblemErrorCode.PROBLEM_NOT_IN_SPACE);
    }

    String normalizedUserAnswer = request.userAnswer().replaceAll("\\s+", "").toLowerCase();

    String normalizedCorrectAnswer = problem.getCorrectAnswer().replaceAll("\\s+", "").toLowerCase();

    boolean isSolved = normalizedUserAnswer.contains(normalizedCorrectAnswer);

    ProblemCounters counters = countersRepository.findByProblemId(problemId)
        .orElseThrow(() -> {
          log.error("[채점 오류] 문제 통계 카운터가 존재하지 않습니다. problemId: {}", problemId);
          return new BusinessException(ProblemErrorCode.COUNTER_NOT_FOUND);
        });

    counters.recordAttempt(isSolved);

    // 유저별 문제 풀이 이력 upsert (싱글모드 대시보드/랭킹 집계의 데이터 소스)
    UserProblemId userProblemId = new UserProblemId(userId, problemId);
    UserProblem userProblem = userProblemRepository.findById(userProblemId).orElse(null);
    if (userProblem != null) {
      userProblem.updateAttempt(isSolved, request.userAnswer());
    } else {
      userProblemRepository.save(UserProblem.builder()
          .id(userProblemId)
          .user(userRepository.getReferenceById(userId))
          .problem(problem)
          .isSolved(isSolved)
          .userAnswer(request.userAnswer())
          .build());
    }

    return new ProblemSolveResponse(
        isSolved,
        problem.getCorrectAnswer(),
        problem.getExplanation(),
        counters.getTryCount(),
        counters.getSolvedCount()
    );
  }

  /**
   * AI 기반 문제 자동 생성 (ADMIN 전용)
   * @param spaceId   공간 ID
   * @param request   AI 문제 자동 생성 요청 DTO
   */
  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED) // 해당 메서드 자체는 트랜잭션 없이 실행
  public List<ProblemResponse> createProblemsByAi(UUID spaceId, ProblemAiCreateRequest request) {
    
    // 공간 / 카테고리 검증
    if (!spaceRepository.existsById(spaceId)) {
      throw new BusinessException(ProblemErrorCode.SPACE_NOT_FOUND);
    }

    if (!categoryRepository.existsById(request.categoryId())) {
      throw new BusinessException(ProblemErrorCode.CATEGORY_NOT_FOUND);
    }
    
    // AI 생성 호출 (트랜잭션 밖 - 커넥션 안 잡고 LLM 응답 대기)
    List<GeneratedProblemData> generated = problemGenerationService.generateProblems(
        request.referenceText(),
        request.questionCount()
    );
    
    return problemPersister.saveGeneratedProblems(spaceId, request.categoryId(), generated);
  }
}
