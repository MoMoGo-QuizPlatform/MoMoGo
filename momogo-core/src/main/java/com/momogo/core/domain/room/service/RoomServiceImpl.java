package com.momogo.core.domain.room.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.room.dto.request.ProblemAnswerRequest;
import com.momogo.core.domain.room.dto.request.RoomAnswerSubmitRequest;
import com.momogo.core.domain.room.dto.request.RoomCreateRequest;
import com.momogo.core.domain.room.dto.response.ProblemGradeReport;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
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
  private final RoomMapper roomMapper;
  private final ApplicationEventPublisher eventPublisher;

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

    // 4. RoomProblem 생성 및 저장
    List<RoomProblem> roomProblems = request.problems().stream()
        .map(prob -> RoomProblem.of(
            savedRoom,
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
  public List<RoomProblemResponse> getRoomProblems(UUID userId, UUID roomId) {
    log.info("[RoomService] 시험 문제지 조회 - userId: {}, roomId: {}", userId, roomId);

    // 방 존재 검증
    Room room = findRoomOrThrow(roomId);

    // 시간 검증 - 현재 시각이 시험 시작 시각 이전인지 확인
    if (OffsetDateTime.now().isBefore(room.getTestStartAt())) {
      log.warn("[RoomService] 시험 시작 전 문제 조회 차단 - roomId: {}, testStartAt: {}", roomId, room.getTestStartAt());
      throw new BusinessException(RoomErrorCode.INVALID_ACCESS_BEFORE_START);
    }

    // 응시 대상 유저 자격 검증 (RoomUser 매핑 테이블 존재 확인)
    RoomUserId roomUserId = new RoomUserId(roomId, userId);
    if (!roomUserRepository.existsById(roomUserId)) {
      throw new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT);
    }

    // 문제 목록 조회 및 정렬 반환
    List<RoomProblem> roomProblems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);

    return roomMapper.toProblemResponseList(roomProblems);
  }

  @Override
  @Transactional
  public void submitRoomAnswer(UUID userId, UUID roomId, RoomAnswerSubmitRequest request) {
    log.info("[RoomService] 시험 답안 제출 시작 - userId: {}, roomId: {}", userId, roomId);

    // 요청 리스트 내 동일한 문제 ID가 중복 유입되는지 검증
    long uniqueProblemCount = request.answers().stream()
        .map(ProblemAnswerRequest::roomProblemId)
        .distinct().count();
    if (uniqueProblemCount != request.answers().size()) {
      throw new BusinessException(RoomErrorCode.DUPLICATE_ANSWER_SUBMITTED);
    }

    // 방 존재 검증
    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

    // 시간 검증 - 시험 시작 전 제출 시도 차단
    OffsetDateTime now = OffsetDateTime.now();
    if (now.isBefore(room.getTestStartAt())) {
      throw new BusinessException(RoomErrorCode.INVALID_ACCESS_BEFORE_START);
    }

    // 상태 검증 - 이미 최종적으로 끝난 시험이면 추가 제출 불가
    if (Boolean.TRUE.equals(room.getIsEnded())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 응시 대상 유저  자격 검증
    RoomUserId roomUserId = new RoomUserId(roomId, userId);
    RoomUser roomUser = roomUserRepository.findById(roomUserId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT));

    // 요청 유저 획득
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));

    List<UUID> problemIds = request.answers().stream().map(ProblemAnswerRequest::roomProblemId).toList();
    Map<UUID, RoomProblem> problemsById = roomProblemRepository.findAllById(problemIds).stream()
        .collect(Collectors.toMap(RoomProblem::getId, Function.identity()));

    if (Boolean.TRUE.equals(roomUser.getIsAttended())) {
      throw new BusinessException(RoomErrorCode.ALREADY_ENDED);
    }

    // 각 문제 답안 일괄 매핑, 저장
    List<UserRoomAnswer> userRoomAnswers = request.answers().stream()
        .map(ans -> {
          RoomProblem problem = problemsById.get(ans.roomProblemId());
          if (problem == null || !problem.getRoom().getId().equals(roomId)) {
            throw new BusinessException(RoomErrorCode.PROBLEM_NOT_FOUND);
          }
          return UserRoomAnswer.of(user, problem, ans.userAnswer(), null);
        })
        .toList();
    userRoomAnswerRepository.saveAll(userRoomAnswers);

    // 응시 완료 상태 업데이트
    roomUser.attend();

    log.info("[RoomService] 시험 답안 제출 완료 - userId: {}, roomId: {}, 제출 문항 수: {}",
        userId, roomId, userRoomAnswers.size());
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
            String userAnswer = ans.getUserAnswer() != null ? ans.getUserAnswer().trim() : "";
            String correctAnswer = problem.getCorrectAnswer() != null ? problem.getCorrectAnswer().trim() : "";
            Boolean isCorrect = userAnswer.equals(correctAnswer);

            // 개별 답안 엔티티에 정답 오답 판정 결과를 주입하고 일괄 업데이트 대상에 추가
            ans.grade(isCorrect);
            answersToUpdate.add(ans);
            if (isCorrect) {
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
      Document document = new Document();
      PdfWriter.getInstance(document, out);

      document.open();

      // 한글 폰트 세팅 - 내장 한글 고딕 폰트 지정
      BaseFont baseFont = BaseFont.createFont("HYGoThic-Medium", "UniKS-UCS2-H", BaseFont.NOT_EMBEDDED);
      Font fontTitle = new Font(baseFont, 16, Font.BOLD);
      Font fontBody = new Font(baseFont, 11, Font.NORMAL);

      // PDF 타이틀 및 기본 정보 기재
      document.add(new Paragraph("==================================================", fontBody));
      document.add(new Paragraph("             MoMoGo Exam Evaluation Report        ", fontTitle));
      document.add(new Paragraph("==================================================", fontBody));
      document.add(new Paragraph(" ", fontBody));
      document.add(new Paragraph("Exam Room: " + report.roomName(), fontBody));
      document.add(new Paragraph("Room ID: " + report.roomId().toString(), fontBody));
      document.add(new Paragraph(" ", fontBody));
      document.add(new Paragraph("------------------ Statistics ------------------", fontBody));
      document.add(new Paragraph("Total Applicants : " + report.totalApplicants(), fontBody));
      document.add(new Paragraph("Attended Count   : " + report.attendedCount(), fontBody));
      document.add(new Paragraph("Average Score    : " + report.averageScore(), fontBody));
      document.add(new Paragraph("Maximum Score    : " + report.maxScore(), fontBody));
      document.add(new Paragraph("-------------------------------------------------", fontBody));
      document.add(new Paragraph(" ", fontBody));

      // 응시자 목록 헤더 추가
      document.add(new Paragraph("Taker Grade Matrix:", fontBody));
      for (TakerGradeReport grade : report.takerGrades()) {
        String status = Boolean.TRUE.equals(grade.isAttended()) ? "Attended" : "Absent";
        document.add(new Paragraph(String.format(" - %s (%s) | Status: %s | Score: %d",
            grade.name(), grade.email(), status, grade.score()), fontBody));
      }

      document.close();
      return out.toByteArray();

    } catch (IOException | DocumentException e) {
      log.error("[RoomService] PDF 생성 중 입출력/문서 포맷팅 예외 발생 - roomId: {}", roomId, e);
      throw new BusinessException(RoomErrorCode.REPORT_GENERATION_FAILED);
    }
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
