package com.momogo.core.domain.room.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.problem.exception.ProblemErrorCode;
import com.momogo.core.domain.problem.repository.ProblemCategoryRepository;
import com.momogo.core.domain.problem.service.ProblemGenerationService;
import com.momogo.core.domain.room.dto.request.ManualGradeRequest;
import com.momogo.core.domain.room.dto.request.ProblemAnswerRequest;
import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.request.RoomProblemDraftAiRequest;
import com.momogo.core.domain.room.dto.response.AnswerGradingItem;
import com.momogo.core.domain.room.dto.response.ProblemGradeReport;
import com.momogo.core.domain.room.dto.response.RoomGradingResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.dto.response.RoomReportResponse;
import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.dto.response.TakerGradeReport;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import com.momogo.core.domain.room.entity.UserRoomAnswer;
import com.momogo.core.domain.room.event.RoomCreatedEvent;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.kafka.producer.AiGradingProducer;
import com.momogo.core.domain.room.mapper.RoomMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomRepository;
import com.momogo.core.domain.room.repository.RoomUserRepository;
import com.momogo.core.domain.room.repository.UserRoomAnswerRepository;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.momogo.core.common.exception.AuthErrorCode;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.room.event.RoomSubmitEventMessage;
import com.momogo.core.domain.room.kafka.producer.RoomSubmitKafkaProducer;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService{

  private final RoomRepository roomRepository;
  private final RoomProblemRepository roomProblemRepository;
  private final RoomUserRepository roomUserRepository;
  private final UserRepository userRepository;
  private final SpaceRepository spaceRepository;
  private final UserRoomAnswerRepository userRoomAnswerRepository;
  private final ProblemCategoryRepository categoryRepository;
  private final ProblemGenerationService problemGenerationService;
  private final RoomMapper roomMapper;
  private final ApplicationEventPublisher eventPublisher;
  private final AiGradingProducer aiGradingProducer;
  private final RedissonClient redissonClient;
  private final RoomSubmitKafkaProducer roomSubmitKafkaProducer;
  private final RedisTemplate<String, Object> redisTemplate;

  private static final String SUBMIT_LOCK_PREFIX = "lock:room:submit:";
  private static final String SUBMIT_CLAIM_PREFIX = "room:submit:claimed:";
  private static final Duration SUBMIT_CLAIM_TTL = Duration.ofDays(7);

  // 검증된 타겟 객체들을 묶기 위한 private record
  private record ValidatedRoomTarget(Space space, List<User> targetUsers) {}

  @Override
  @Transactional
  public RoomResponse createRoom(UUID userId, UUID spaceId, RoomCreateRequest request) {

    log.info("[RoomService] 평가 시험방 생성 시작 - userId: {}, spaceId: {}, roomName: {}", userId, spaceId, request.name());

    // 1. 유효성 검증 및 데이터 조회
    ValidatedRoomTarget targets = validateAndGetTargets(userId, spaceId, request);

    // 2. Room 생성 및 저장
    Room savedRoom = roomRepository.save(Room.of(
        targets.space(),
        request.name(),
        request.description(),
        request.testStartAt(),
        request.testEndAt()
    ));

    // 3. RoomUser 매핑 관계 생성 및 저장
    List<RoomUser> roomUsers = targets.targetUsers().stream()
        .map(targetUser -> RoomUser.of(savedRoom, targetUser))
        .toList();
    roomUserRepository.saveAll(roomUsers);

    // 4. 문제별 카테고리 일괄 조회 및 검증
    List<UUID> categoryIds = request.problems().stream()
        .map(com.momogo.core.domain.room.dto.request.RoomProblemCreatedRequest::categoryId)
        .distinct()
        .toList();
    Map<UUID, ProblemCategory> categoriesById = categoryRepository.findAllById(categoryIds).stream()
        .collect(Collectors.toMap(ProblemCategory::getId, Function.identity()));
    if (categoriesById.size() != categoryIds.size()) {
      throw new BusinessException(ProblemErrorCode.CATEGORY_NOT_FOUND);
    }

    // 5. RoomProblem 생성 및 저장
    List<RoomProblem> roomProblems = request.problems().stream()
        .map(prob -> RoomProblem.of(
            savedRoom,
            categoriesById.get(prob.categoryId()),
            prob.problemOrder(),
            prob.name(),
            prob.content(),
            prob.explanation(),
            prob.correctAnswer()))
        .toList();
    roomProblemRepository.saveAll(roomProblems);

    log.info("[RoomService] 평가 시험방 생성 성공 - roomId: {}", savedRoom.getId());

    // 알림용 Spring Event 발행
    eventPublisher.publishEvent(new RoomCreatedEvent(
        savedRoom.getId(),
        targets.space.getId(),
        savedRoom.getName(),
        request.userIds()
    ));

    return roomMapper.toResponse(savedRoom);
  }

  @Override
  public List<GeneratedProblemData> generateDraftProblems(UUID userId, UUID spaceId, RoomProblemDraftAiRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }

    return problemGenerationService.generateProblems(request.referenceText(), request.questionCount());
  }

  @Override
  public RoomResponse getRoomDetails(UUID userId, UUID roomId) {
    log.info("[RoomService] 시험방 상세 조회 - userId: {}, roomId: {}", userId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 권한 검증 - 요청 유저가 해당 방의 소속 공간 멤버가 맞는지 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getSpace() == null || !user.getSpace().getId().equals(room.getSpace().getId())) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER);
    }

    return  roomMapper.toResponse(room);
  }

  @Override
  public List<RoomResponse> getRoomList(UUID userId, UUID spaceId) {
    log.info("[RoomService] 시험방 목록 조회 - userId: {}, spaceId: {}", userId, spaceId);

    // 공간 존재 검증
    if (!spaceRepository.existsById(spaceId)) {
      throw new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND);
    }

    // 권한 검증 - 요청 유저가 해당 공간 소속 멤버가 맞는지 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER);
    }

    List<Room> rooms = roomRepository.findAllBySpaceIdOrderByCreatedAtDesc(spaceId);

    return roomMapper.toResponseList(rooms);
  }

  @Override
  public List<RoomProblemResponse> getRoomProblems(UUID userId, UUID roomId) {
    log.info("[RoomService] 시험 문제지 조회 - userId: {}, roomId: {}", userId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 시간 검증 - 현재 시각이 시험 시작 시각 이전인지 확인
    if (OffsetDateTime.now().isBefore(room.getTestStartAt())) {
      log.warn("[RoomService] 시험 시작 전 문제 조회 차단 - roomId: {}, testStartAt: {}", roomId, room.getTestStartAt());
      throw new BusinessException(RoomErrorCode.INVALID_ACCESS_BEFORE_START);
    }

    // 상태 검증 - 이미 마감된 시험은 재입장 차단
    if (Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 응시 대상 유저 자격 검증 (RoomUser 매핑 테이블 존재 확인)
    RoomUserId roomUserId = new RoomUserId(roomId, userId);
    RoomUser roomUser = roomUserRepository.findById(roomUserId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT));

    // 상태 검증 - 이미 답안을 제출한 응시자의 재입장(재조회) 차단
    if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 문제 목록 조회 및 정렬 반환
    List<RoomProblem> roomProblems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);

    return roomMapper.toProblemResponseList(roomProblems);
  }

  @Override
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void submitRoomAnswer(UUID userId, UUID roomId, RoomAnswerSubmitRequest request) {
    log.info("[RoomService] 시험 답안 제출 시도 - userId: {}, roomId: {}", userId, roomId);

    // 1. 요청 리스트 내 동일한 문제 ID가 중복 유입되는지 검증
    long uniqueProblemCount = request.answers().stream()
        .map(ProblemAnswerRequest::roomProblemId)
        .distinct().count();
    if (uniqueProblemCount != request.answers().size()) {
      throw new BusinessException(RoomErrorCode.DUPLICATE_ANSWER_SUBMITTED);
    }

    // 2. Redisson 분산 락 획득 (Redisson Watchdog 활용을 위해 leaseTime 생략)
    String lockKey = SUBMIT_LOCK_PREFIX + roomId + ":" + userId;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      if (lock.tryLock(3, TimeUnit.SECONDS)) {
        try {
          // 3. 비관적 락(findByIdForUpdate) 대신 락이 없는 일반 조회 사용
          Room room = findRoomOrThrow(roomId);

          // 4. 시간 및 응시 자격 빠른 검증
          OffsetDateTime now = OffsetDateTime.now();
          if (now.isBefore(room.getTestStartAt())) {
            throw new BusinessException(RoomErrorCode.INVALID_ACCESS_BEFORE_START);
          }

          if (Boolean.TRUE.equals(room.getIsEnded())) {
            throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
          }

          RoomUserId roomUserId = new RoomUserId(roomId, userId);
          RoomUser roomUser = roomUserRepository.findById(roomUserId)
              .orElseThrow(() -> new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT));

          if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
            throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
          }

          // 4-1. 비동기 저장 완료 전 중복 제출을 즉시 차단하는 Redis Claim 마커 (Atomic SETNX)
          String claimKey = SUBMIT_CLAIM_PREFIX + roomId + ":" + userId;
          Boolean claimed = redisTemplate.opsForValue().setIfAbsent(claimKey, "1", SUBMIT_CLAIM_TTL);
          if (!Boolean.TRUE.equals(claimed)) {
            log.warn("[RoomService] 이미 답안 제출 요청이 처리 중이거나 완료된 유저 - userId: {}, roomId: {}", userId, roomId);
            throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
          }

          // 5. 문제 존재 및 해당 시험방 소속 검증
          List<UUID> problemIds = request.answers().stream()
              .map(ProblemAnswerRequest::roomProblemId)
              .toList();

          long validProblemCount = roomProblemRepository.findAllById(problemIds).stream()
              .filter(p -> p.getRoom().getId().equals(roomId))
              .count();

          if (validProblemCount != problemIds.size()) {
            throw new BusinessException(RoomErrorCode.PROBLEM_NOT_FOUND);
          }

          // 6. 동기 DB 저장 대신 Kafka 전송 후 < 20ms 즉시 응답 반환
          roomSubmitKafkaProducer.send(RoomSubmitEventMessage.of(userId, roomId, request.answers()));

        } finally {
          if (lock.isHeldByCurrentThread()) {
            lock.unlock();
          }
        }
      } else {
        log.warn("[RoomService] 답안 제출 분산 락 획득 타임아웃 - userId: {}, roomId: {}", userId, roomId);
        throw new BusinessException(RoomErrorCode.LOCK_ACQUISITION_FAILED);
      }
    } catch (InterruptedException e) {
      log.error("[RoomService] 답안 제출 대기 중 인터럽트 발생 - userId: {}, roomId: {}", userId, roomId, e);
      Thread.currentThread().interrupt();
      throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "답안 제출 대기 중 인터럽트가 발생했습니다.");
    }
  }

  @Override
  @Transactional
  public void finalizeGrade(UUID adminUserId, UUID roomId) {

    log.info("[RoomService] 시험 채점 최종 마감 시작 - adminUserId: {}, roomId: {}", adminUserId, roomId);

    // 방 존재 검증, 비관적 락 적용
    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

    // 권한 검증 - 마감 요청자가 ADMIN인지 + 방의 소속 공간 관리자가 맞는지 확인
    validateSpaceAdmin(adminUserId, room);

    // 중복 마감 차단 검증
    if (Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // AI 채점이 아직 진행 중이면 확정 차단 (채점 결과 검토 전 확정 방지)
    if (Boolean.TRUE.equals(room.getIsAiGradingInProgress())) {
      throw new BusinessException(RoomErrorCode.AI_GRADING_IN_PROGRESS);
    }

    // 시험 문제 목록 및 응시자 목록 조회
    List<RoomProblem> roomProblems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);
    List<RoomUser> roomUsers = roomUserRepository.findAllByRoomId(roomId);

    if (roomProblems.isEmpty()) {
      log.warn("[RoomService] 출제된 문제가 없는 시험방입니다. roomId: {}", roomId);
      room.finalizeTest();
      return;
    }

    // 벌크 연산1. 해당 방의 모든 응시자 제출 답안을 일괄 조회
    List<UserRoomAnswer> allSubmittedAnswers = userRoomAnswerRepository.findByRoomProblemRoomId(roomId);

    // 벌크 연산2. 모든 답안을 유저 ID별로 그룹핑해서 메모리 맵으로 전환
    Map<UUID, List<UserRoomAnswer>> answersByTakerMap = allSubmittedAnswers.stream()
        .collect(Collectors.groupingBy(ans -> ans.getUser().getId()));

    int totalProblems = roomProblems.size();
    // 일괄 업데이트할 답안 리스트
    List<UserRoomAnswer> answersToUpdate = new ArrayList<>();

    // 메모리 상에서 채점 루프
    for (RoomUser roomUser : roomUsers) {
      UUID testTakerId = roomUser.getUser().getId();

      if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
        List<UserRoomAnswer> submitted = answersByTakerMap.get(testTakerId);

        if (submitted == null) {
          roomUser.updateScore(0);
          continue;
        }

        // 문제별 제출 답안 매핑용 맵 생성
        Map<UUID, UserRoomAnswer> problemAnswerMap = submitted.stream()
            .collect(Collectors.toMap(
                ans -> ans.getRoomProblem().getId(),
                ans -> ans
            ));

        int correctCount = 0;
        for (RoomProblem problem : roomProblems) {
          UserRoomAnswer ans = problemAnswerMap.get(problem.getId());

          if (ans != null) {
            // AI 채점 또는 관리자 수동 채점으로 이미 정오 판정이 끝난 답안은 그 결과를 그대로 신뢰하고,
            // 아직 판정되지 않은(null) 답안만 문자열 완전 일치로 기본 채점한다.
            if (ans.getIsCorrect() == null) {
              String userAnswer = ans.getUserAnswer() != null ? ans.getUserAnswer().trim() : "";
              String correctAnswer = problem.getCorrectAnswer() != null ? problem.getCorrectAnswer().trim() : "";
              ans.grade(userAnswer.equals(correctAnswer));
              answersToUpdate.add(ans);
            }

            if (Boolean.TRUE.equals(ans.getIsCorrect())) {
              correctCount++;
            }
          }
        }

        int finalScore = (int) Math.round((double) correctCount / totalProblems * 100);
        roomUser.updateScore(finalScore);
      } else {
        // 미응시자 0점 처리
        roomUser.updateScore(0);
      }
    }

    // 벌크 연산3. 배치 쓰기(Batch Update) 유도
    roomUserRepository.saveAll(roomUsers);
    // 변경된 정오표 DB 기록
    userRoomAnswerRepository.saveAll(answersToUpdate);

    // 시험 마감 처리
    room.finalizeTest();
    log.info("[RoomService] 시험 채점 최종 마감 및 비활성화 완료 - roomId: {}", roomId);
  }

  @Override
  @Transactional(readOnly = true)
  public RoomReportResponse getRoomReport(UUID adminUserId, UUID roomId) {

    log.info("[RoomService] 시험 리포트 조회 시작 - adminUserId: {}, roomId: {}", adminUserId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 권한 검증 - 마감 요청자가 ADMIN인지 + 방의 소속 공간 관리자가 맞는지 확인
    validateSpaceAdmin(adminUserId, room);

    // 채점이 마감되지 않은 상태이면 리포트 생성 차단
    if (!Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.REPORT_NOT_READY);
    }

    // 문제 목록 및 응시자 리스트 일괄 획득
    List<RoomProblem> roomProblems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);
    List<RoomUser> roomUsers = roomUserRepository.findAllByRoomId(roomId);

    if (roomUsers.isEmpty()) {
      return new RoomReportResponse(roomId, room.getName(), 0, 0, 0.0, 0, List.of());
    }

    // 이 방의 모든 제출 답안 목록 전체 획득
    List<UserRoomAnswer> allAnswers = userRoomAnswerRepository.findByRoomProblemRoomId(roomId);

    // 유저 id별로 제출된 답안 목록 그룹핑
    Map<UUID, List<UserRoomAnswer>> answersByTakerMap = allAnswers.stream()
        .collect(Collectors.groupingBy(ans -> ans.getUser().getId()));

    // 통계 지표 계산 (평균, 최고점, 응시율)
    int totalApplicant = roomUsers.size();
    List<RoomUser> attendedUsers = roomUsers.stream()
        .filter(u -> Boolean.TRUE.equals(u.getIsAttended()))
        .toList();

    int attendedCount = attendedUsers.size();

    double averageScore = attendedCount > 0
        ? attendedUsers.stream().mapToInt(RoomUser::getScore).average().orElse(0.0)
        : 0.0;

    int maxScore = attendedCount > 0
        ? attendedUsers.stream().mapToInt(RoomUser::getScore).max().orElse(0)
        : 0;

    // 유저별 상세 문항 성적표 목록 생성
    List<TakerGradeReport> takerGrades = roomUsers.stream().map(roomUser -> {
      User user = roomUser.getUser();

      List<ProblemGradeReport> problemGrades;
      if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
        List<UserRoomAnswer> submitted = answersByTakerMap.getOrDefault(user.getId(),
            Collections.emptyList());
        Map<UUID, UserRoomAnswer> problemAnswerMap = submitted.stream()
            .collect(Collectors.toMap(ans -> ans.getRoomProblem().getId(), ans -> ans));

        problemGrades = roomProblems.stream().map(problem -> {
          UserRoomAnswer ans = problemAnswerMap.get(problem.getId());
          return new ProblemGradeReport(
              problem.getId(),
              problem.getProblemOrder(),
              problem.getName(),
              ans != null ? ans.getUserAnswer() : "",
              problem.getCorrectAnswer(),
              ans != null && Boolean.TRUE.equals(ans.getIsCorrect())
          );
        }).toList();
      } else {
        problemGrades = roomProblems.stream().map(problem -> new ProblemGradeReport(
            problem.getId(),
            problem.getProblemOrder(),
            problem.getName(),
            "",
            problem.getCorrectAnswer(),
            false
        )).toList();
      }

      return new TakerGradeReport(
          user.getId(),
          user.getName(),
          user.getEmail(),
          user.getProfileImageUrl(),
          roomUser.getIsAttended(),
          roomUser.getScore(),
          problemGrades
      );
    }).toList();

    return new RoomReportResponse(roomId, room.getName(), totalApplicant, attendedCount, averageScore, maxScore, takerGrades);
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] downloadRoomReportPdf(UUID adminUserId, UUID roomId) {

    log.info("[RoomService] 시험 리포트 PDF 다운로드 시작 - adminUserId: {}, roomId: {}", adminUserId, roomId);

    // 성적 리포트 데이터 획득
    RoomReportResponse report = getRoomReport(adminUserId, roomId);

    // OpenPDF 라이브러리를 활용해 바이너리 스트림 생성
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Document document = new Document(PageSize.A4, 40, 40, 50, 50);
      PdfWriter.getInstance(document, out);

      document.open();

      // 브랜드 컬러 및 한글 폰트 세팅 - 내장 한글 고딕 폰트 지정
      Color primaryColor = new Color(99, 102, 241);
      Color headerBg = new Color(238, 238, 253);
      Color rowAltBg = new Color(247, 247, 250);
      Color borderColor = new Color(220, 220, 230);

      // PDF 뷰어가 CJK 시스템 폰트를 갖고 있지 않으면 글자가 안 보이는 문제가 있어(NOT_EMBEDDED 폰트 한계)
      // 폰트 파일 자체를 PDF에 임베드한다.
      BaseFont baseFontRegular = loadEmbeddedKoreanFont("/fonts/NanumGothic-Regular.ttf");
      BaseFont baseFontBold = loadEmbeddedKoreanFont("/fonts/NanumGothic-Bold.ttf");

      Font fontTitle = new Font(baseFontBold, 20, Font.NORMAL, Color.WHITE);
      Font fontSectionTitle = new Font(baseFontBold, 13, Font.NORMAL, primaryColor);
      Font fontLabel = new Font(baseFontRegular, 10, Font.NORMAL, Color.GRAY);
      Font fontValue = new Font(baseFontBold, 14, Font.NORMAL, Color.DARK_GRAY);
      Font fontTableHeader = new Font(baseFontBold, 11, Font.NORMAL, Color.WHITE);
      Font fontTableBody = new Font(baseFontRegular, 11, Font.NORMAL, Color.DARK_GRAY);

      // 상단 타이틀 배너 (시험방 이름 표시 - ID가 아닌 실제 이름)
      PdfPTable titleBanner = new PdfPTable(1);
      titleBanner.setWidthPercentage(100);
      PdfPCell titleCell = new PdfPCell(new Paragraph(report.roomName(), fontTitle));
      titleCell.setBackgroundColor(primaryColor);
      titleCell.setBorder(Rectangle.NO_BORDER);
      titleCell.setPadding(16f);
      titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
      titleBanner.addCell(titleCell);
      document.add(titleBanner);

      Paragraph subtitle = new Paragraph("MoMoGo 정기 평가시험 결과 리포트", fontLabel);
      subtitle.setAlignment(Element.ALIGN_CENTER);
      subtitle.setSpacingBefore(8f);
      subtitle.setSpacingAfter(20f);
      document.add(subtitle);

      // 요약 통계 카드 (4열)
      document.add(new Paragraph("통계 요약", fontSectionTitle));
      document.add(new Paragraph(" ", fontLabel));

      PdfPTable statTable = new PdfPTable(4);
      statTable.setWidthPercentage(100);
      statTable.setSpacingAfter(20f);
      addStatCell(statTable, "총 응시 대상", report.totalApplicants() + "명", headerBg, borderColor, fontLabel, fontValue);
      addStatCell(statTable, "실제 응시 인원", report.attendedCount() + "명", headerBg, borderColor, fontLabel, fontValue);
      addStatCell(statTable, "평균 점수", String.format("%.1f점", report.averageScore()), headerBg, borderColor, fontLabel, fontValue);
      addStatCell(statTable, "최고 점수", report.maxScore() + "점", headerBg, borderColor, fontLabel, fontValue);
      document.add(statTable);

      // 응시자별 성적 상세 표
      document.add(new Paragraph("응시자별 성적", fontSectionTitle));
      document.add(new Paragraph(" ", fontLabel));

      PdfPTable gradeTable = new PdfPTable(4);
      gradeTable.setWidthPercentage(100);
      gradeTable.setWidths(new float[]{3f, 4f, 2f, 2f});
      gradeTable.setHeaderRows(1);

      for (String header : new String[]{"이름", "이메일", "응시 여부", "점수"}) {
        PdfPCell headerCell = new PdfPCell(new Paragraph(header, fontTableHeader));
        headerCell.setBackgroundColor(primaryColor);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setPadding(8f);
        headerCell.setBorderColor(borderColor);
        gradeTable.addCell(headerCell);
      }

      int rowIndex = 0;
      for (TakerGradeReport grade : report.takerGrades()) {
        boolean isAttended = Boolean.TRUE.equals(grade.isAttended());
        Color rowBg = rowIndex % 2 == 0 ? Color.WHITE : rowAltBg;

        gradeTable.addCell(buildBodyCell(grade.name(), fontTableBody, rowBg, borderColor, Element.ALIGN_LEFT));
        gradeTable.addCell(buildBodyCell(grade.email(), fontTableBody, rowBg, borderColor, Element.ALIGN_LEFT));
        gradeTable.addCell(buildBodyCell(isAttended ? "응시 완료" : "결시", fontTableBody, rowBg, borderColor, Element.ALIGN_CENTER));
        gradeTable.addCell(buildBodyCell(isAttended ? grade.score() + "점" : "-", fontTableBody, rowBg, borderColor, Element.ALIGN_CENTER));
        rowIndex++;
      }
      document.add(gradeTable);

      document.close();
      return out.toByteArray();

    } catch (IOException | DocumentException e) {
      log.error("[RoomService] PDF 생성 중 입출력/문서 포맷팅 예외 발생 - roomId: {}", roomId, e);
      throw new BusinessException(RoomErrorCode.REPORT_GENERATION_FAILED);
    }
  }

  // 요약 통계 카드 한 칸(라벨 + 값) 생성 헬퍼
  private void addStatCell(PdfPTable table, String label, String value, Color bg, Color border, Font labelFont, Font valueFont) {
    PdfPTable inner = new PdfPTable(1);
    inner.setWidthPercentage(100);

    PdfPCell labelCell = new PdfPCell(new Paragraph(label, labelFont));
    labelCell.setBorder(Rectangle.NO_BORDER);
    labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    labelCell.setPaddingBottom(4f);
    inner.addCell(labelCell);

    PdfPCell valueCell = new PdfPCell(new Paragraph(value, valueFont));
    valueCell.setBorder(Rectangle.NO_BORDER);
    valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
    inner.addCell(valueCell);

    PdfPCell outer = new PdfPCell(inner);
    outer.setBackgroundColor(bg);
    outer.setBorderColor(border);
    outer.setPadding(12f);
    table.addCell(outer);
  }

  // 응시자 성적 표 본문 셀 생성 헬퍼
  private PdfPCell buildBodyCell(String text, Font font, Color bg, Color border, int align) {
    PdfPCell cell = new PdfPCell(new Paragraph(text, font));
    cell.setBackgroundColor(bg);
    cell.setBorderColor(border);
    cell.setHorizontalAlignment(align);
    cell.setPadding(8f);
    return cell;
  }

  // PDF 리포트용 한글 폰트를 클래스패스 리소스에서 읽어 PDF에 임베드(NOT_EMBEDDED 시스템 폰트 의존 문제 회피)
  private BaseFont loadEmbeddedKoreanFont(String resourcePath) throws IOException, DocumentException {
    try (InputStream fontStream = getClass().getResourceAsStream(resourcePath)) {
      if (fontStream == null) {
        throw new IOException("PDF 리포트용 폰트 리소스를 찾을 수 없습니다: " + resourcePath);
      }
      byte[] fontBytes = fontStream.readAllBytes();
      return BaseFont.createFont(resourcePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
    }
  }

  @Override
  @Transactional
  public void startAiGrading(UUID adminUserId, UUID roomId) {

    log.info("[RoomService] AI 선제 채점 비동기 요청 - adminUserId: {}, roomId: {}", adminUserId, roomId);

    // 방 및 관리자 권한 검증 (동시 트리거 방지 비관적 락 조회)
    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
    validateSpaceAdmin(adminUserId, room);

    // 이미 성적 채점 및 마감이 완료된 시험방은 예외 처리
    if (Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 이미 AI 채점이 진행 중이면 중복 트리거 차단
    if (Boolean.TRUE.equals(room.getIsAiGradingInProgress())) {
      throw new BusinessException(RoomErrorCode.AI_GRADING_IN_PROGRESS);
    }

    room.markAiGradingInProgress();

    // Kafka Producer 메시지 발행
    aiGradingProducer.sendAiGradingEvent(roomId, null);
  }

  @Override
  public RoomGradingResponse getRoomGrading(UUID adminUserId, UUID roomId) {

    log.info("[RoomService] 채점 검토 화면 조회 - adminUserId: {}, roomId: {}", adminUserId, roomId);

    // 방 존재 및 관리자 권한 검증
    Room room = findRoomOrThrow(roomId);
    validateSpaceAdmin(adminUserId, room);

    // 응시자별 문제별 제출 답안 전체 조회 (문제 순서 -> 응시자 이름 순 정렬)
    List<UserRoomAnswer> answers = userRoomAnswerRepository.findByRoomProblemRoomId(roomId);
    List<AnswerGradingItem> items = answers.stream()
        .sorted(Comparator
            .comparing((UserRoomAnswer a) -> a.getRoomProblem().getProblemOrder())
            .thenComparing(a -> a.getUser().getName()))
        .map(a -> new AnswerGradingItem(
            a.getId(),
            a.getUser().getId(),
            a.getUser().getName(),
            a.getUser().getProfileImageUrl(),
            a.getRoomProblem().getId(),
            a.getRoomProblem().getProblemOrder(),
            a.getRoomProblem().getName(),
            a.getUserAnswer(),
            a.getRoomProblem().getCorrectAnswer(),
            a.getIsCorrect()
        ))
        .toList();

    return new RoomGradingResponse(room.getId(), room.getName(), room.getIsAiGradingInProgress(), items);
  }

  @Override
  @Transactional
  public void manualGradeAnswer(UUID adminUserId, UUID roomId, UUID answerId, ManualGradeRequest request) {

    log.info("[RoomService] 수동 채점 오버라이드 - adminUserId: {}, roomId: {}, answerId: {}, isCorrect: {}",
        adminUserId, roomId, answerId, request.isCorrect());

    // 방 존재 및 관리자 권한 검증
    Room room = findRoomOrThrow(roomId);
    validateSpaceAdmin(adminUserId, room);

    // 이미 채점 확정(마감)된 시험방은 수동 채점 변경 불가
    if (Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 답안 존재 검증 및 해당 방 소속 여부 검증
    UserRoomAnswer answer = userRoomAnswerRepository.findById(answerId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.PROBLEM_NOT_FOUND));
    if (!answer.getRoomProblem().getRoom().getId().equals(roomId)) {
      throw new BusinessException(RoomErrorCode.PROBLEM_NOT_FOUND);
    }

    answer.manualGrade(request.isCorrect());
  }

  @Override
  @Transactional
  public void saveGradingResults(Map<UUID, Boolean> gradingResults, UUID roomId) {
    for (Map.Entry<UUID, Boolean> entry : gradingResults.entrySet()) {
      UUID answerId = entry.getKey();
      Boolean isCorrect = entry.getValue();
      if (isCorrect != null) {
        // 수동 채점이 수행되지 않은 답안에 대해서만 DB 단에서 원자적으로(Atomic) 정답 반영 (수동 채점 오버라이드 레이스 조건 차단)
        int updatedCount = userRoomAnswerRepository.updateIsCorrectIfNotManuallyGraded(answerId, isCorrect);
        if (updatedCount == 0) {
          log.info("[RoomService] 수동 채점 완료 또는 존재하지 않는 답안 AI 결과 반영 스킵 - answerId: {}", answerId);
        }
      }
    }
    // 채점 완료 상태 해제
    roomRepository.findByIdForUpdate(roomId).ifPresent(Room::clearAiGradingInProgress);
  }

  @Override
  @Transactional
  public void clearAiGradingStatus(UUID roomId) {
    roomRepository.findByIdForUpdate(roomId).ifPresent(Room::clearAiGradingInProgress);
  }

  // 시험방 생성을 위한 유효성 검증 및 도메인 엔티티 일괄 조회 헬퍼 메서드
  private ValidatedRoomTarget validateAndGetTargets(UUID userId, UUID spaceId, RoomCreateRequest request) {

    // 개설 유저의 공간 관리자(ADMIN) 권한 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));


    // 공간(Space) 존재 여부 검증 및 획득
    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));


    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }

    // 응시 대상 유저 검증 및 획득
    List<User> targetUsers = userRepository.findAllById(request.userIds());
    if (targetUsers.size() != request.userIds().size()) {
      log.warn("[RoomService] 응시 대상 유저 중 일부가 존재하지 않습니다. 요청: {}, 조회: {}",
          request.userIds().size(), targetUsers.size());
      throw new BusinessException(RoomErrorCode.ROOM_USER_NOT_FOUND);
    }

    // 공간 경계 검증 (모든 응시 유저가 이 공간 소속인지 확인)
    boolean hasExternalUser = targetUsers.stream()
        .anyMatch(u -> u.getSpace() == null || !u.getSpace().getId().equals(spaceId));

    if (hasExternalUser) {
      log.warn("[RoomService] 해당 공간 소속이 아닌 외부 유저가 응시 대상에 포함되어 있습니다. spaceId: {}", spaceId);
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_MEMBER);
    }

    return new ValidatedRoomTarget(space, targetUsers);
  }

  private Room findRoomOrThrow(UUID roomId) {
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));
  }

  private void validateSpaceAdmin(UUID adminUserId, Room room) {
    // 권한 검증
    User adminUser = userRepository.findById(adminUserId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    if (adminUser.getRole() != UserRole.ADMIN || adminUser.getSpace() == null || !adminUser.getSpace().getId().equals(room.getSpace().getId())) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }
  }
}
