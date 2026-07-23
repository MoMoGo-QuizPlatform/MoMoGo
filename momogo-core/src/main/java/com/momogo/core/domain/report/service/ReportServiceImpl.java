package com.momogo.core.domain.report.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.report.dto.response.DashboardSummaryResponse;
import com.momogo.core.domain.report.dto.response.MyExamDetailResponse;
import com.momogo.core.domain.report.dto.response.MyExamListItemResponse;
import com.momogo.core.domain.report.dto.response.MyExamProblemResultResponse;
import com.momogo.core.domain.report.dto.response.PersonalReportResponse;
import com.momogo.core.domain.report.dto.response.SingleModeHistoryResponse;
import com.momogo.core.domain.report.dto.response.SingleModeSummaryResponse;
import com.momogo.core.domain.report.dto.response.SpaceRankingResponse;
import com.momogo.core.domain.report.entity.ReportType;
import com.momogo.core.domain.report.mapper.ReportMapper;
import com.momogo.core.domain.report.repository.PersonalReportRepository;
import com.momogo.core.domain.report.repository.SpaceRankingRepository;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import com.momogo.core.domain.room.entity.UserRoomAnswer;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomUserRepository;
import com.momogo.core.domain.room.repository.UserRoomAnswerRepository;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.UserProblem;
import com.momogo.core.domain.user.repository.UserProblemRepository;
import com.momogo.core.domain.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

  private final PersonalReportRepository personalReportRepository;
  private final SpaceRankingRepository spaceRankingRepository;
  private final ReportMapper reportMapper;
  private final UserProblemRepository userProblemRepository;
  private final RoomUserRepository roomUserRepository;
  private final RoomProblemRepository roomProblemRepository;
  private final UserRoomAnswerRepository userRoomAnswerRepository;
  private final UserRepository userRepository;

  @Override
  public PersonalReportResponse getPersonalReport(UUID userId, ReportType reportType,
      LocalDate reportDate) {
    LocalDate targetDate = reportType == ReportType.WEEKLY
        ? reportDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        : reportDate;

    // 배치가 아직 안 돌았거나(오늘 첫 문제 풀이 전 등) 그날 활동이 없어서 리포트 행 자체가
    // 없을 수 있음. 이 경우 404를 던지는 대신, "활동 없음"을 뜻하는 0짜리 응답을 만들어 반환함
    // (대시보드 입장에서는 에러가 아니라 그냥 텅 빈 하루/주로 보여주는 게 자연스러움)
    return personalReportRepository.findByUserIdAndReportTypeAndReportDate(userId, reportType,
            targetDate)
        .map(reportMapper::toResponse)
        .orElseGet(() -> new PersonalReportResponse(null, reportType, targetDate, 0, 0, 0.0, null));
  }

  @Override
  public List<SpaceRankingResponse> getSpaceRanking(UUID spaceId) {
    // "이번 주"의 시작 시각(월요일 00:00)을 구해서, 그 이후에 풀린 문제만 랭킹에 반영함
    // (전체 누적이 아니라 "이번 주" 랭킹이라는 요구사항이라, 매주 월요일이 되면 자연스럽게 초기화됨)
    OffsetDateTime periodStart = startOfThisWeek();

    return spaceRankingRepository.findRankingBySpaceId(spaceId, periodStart);
  }

  @Override
  public DashboardSummaryResponse getDashboardSummary(UUID userId) {
    UUID spaceId = resolveCurrentSpaceId(userId);
    if (spaceId == null) {
      return new DashboardSummaryResponse(0, 0.0, 0);
    }

    OffsetDateTime todayStart = startOfToday();
    OffsetDateTime todayEnd = todayStart.plusDays(1);
    OffsetDateTime weekStart = startOfThisWeek();
    OffsetDateTime now = OffsetDateTime.now();

    long todaySolvedCount = userProblemRepository.countSolvedByUserAndSpaceAndPeriod(userId, spaceId, todayStart, todayEnd);

    long weeklyAttempted = userProblemRepository.countAttemptedByUserAndSpaceAndPeriod(userId, spaceId, weekStart, now);
    long weeklySolved = userProblemRepository.countSolvedByUserAndSpaceAndPeriod(userId, spaceId, weekStart, now);
    double weeklyAccuracyRate = calculateAccuracyRate(weeklySolved, weeklyAttempted);

    long completedExamCount = roomUserRepository.countByUser_IdAndIsAttendedTrueAndRoom_Space_Id(userId, spaceId);

    return new DashboardSummaryResponse(todaySolvedCount, weeklyAccuracyRate, completedExamCount);
  }

  @Override
  public SingleModeSummaryResponse getSingleModeSummary(UUID userId) {
    UUID spaceId = resolveCurrentSpaceId(userId);
    if (spaceId == null) {
      return new SingleModeSummaryResponse(0, 0, 0.0);
    }

    OffsetDateTime todayStart = startOfToday();
    OffsetDateTime todayEnd = todayStart.plusDays(1);
    OffsetDateTime weekStart = startOfThisWeek();
    OffsetDateTime now = OffsetDateTime.now();

    long dailySolvedCount = userProblemRepository.countSolvedByUserAndSpaceAndPeriod(userId, spaceId, todayStart, todayEnd);
    long weeklySolvedCount = userProblemRepository.countSolvedByUserAndSpaceAndPeriod(userId, spaceId, weekStart, now);

    long totalAttempted = userProblemRepository.countByUser_IdAndProblem_Space_Id(userId, spaceId);
    long totalSolved = userProblemRepository.countByUser_IdAndProblem_Space_IdAndIsSolvedTrue(userId, spaceId);
    double averageCorrectRate = calculateAccuracyRate(totalSolved, totalAttempted);

    return new SingleModeSummaryResponse(dailySolvedCount, weeklySolvedCount, averageCorrectRate);
  }

  @Override
  public List<SingleModeHistoryResponse> getSingleModeHistory(UUID userId) {
    UUID spaceId = resolveCurrentSpaceId(userId);
    if (spaceId == null) {
      return List.of();
    }

    return userProblemRepository.findAllByUser_IdAndProblem_Space_IdOrderByCreatedAtDesc(userId, spaceId).stream()
        .map(up -> new SingleModeHistoryResponse(
            up.getProblem().getId(),
            up.getProblem().getName(),
            up.getProblem().getCategory().getName(),
            Boolean.TRUE.equals(up.getIsSolved()),
            up.getUserAnswer(),
            up.getProblem().getCorrectAnswer(),
            up.getCreatedAt()
        ))
        .toList();
  }

  @Override
  public List<MyExamListItemResponse> getMyExamList(UUID userId) {
    UUID spaceId = resolveCurrentSpaceId(userId);
    if (spaceId == null) {
      return List.of();
    }

    return roomUserRepository.findAllByUser_IdAndRoom_Space_Id(userId, spaceId).stream()
        .filter(ru -> Boolean.TRUE.equals(ru.getIsAttended()))
        .map(ru -> {
          Room room = ru.getRoom();
          int totalProblems = (int) roomProblemRepository.countByRoomId(room.getId());
          return new MyExamListItemResponse(
              room.getId(),
              room.getName(),
              room.getDescription(),
              ru.getScore(),
              totalProblems,
              room.getTestStartAt(),
              room.getTestEndAt(),
              Boolean.TRUE.equals(room.getIsEnded())
          );
        })
        .toList();
  }

  @Override
  public MyExamDetailResponse getMyExamDetail(UUID userId, UUID roomId) {
    RoomUser roomUser = roomUserRepository.findById(new RoomUserId(roomId, userId))
        .orElseThrow(() -> new BusinessException(RoomErrorCode.NOT_ROOM_PARTICIPANT));

    Room room = roomUser.getRoom();

    List<RoomProblem> roomProblems = roomProblemRepository.findByRoomIdOrderByProblemOrder(roomId);
    List<UserRoomAnswer> myAnswers = userRoomAnswerRepository.findByRoomProblemRoomIdAndUserId(roomId, userId);
    Map<UUID, UserRoomAnswer> answerByProblemId = myAnswers.stream()
        .collect(Collectors.toMap(a -> a.getRoomProblem().getId(), Function.identity()));

    List<MyExamProblemResultResponse> problems = roomProblems.stream()
        .map(problem -> {
          UserRoomAnswer answer = answerByProblemId.get(problem.getId());
          return new MyExamProblemResultResponse(
              problem.getId(),
              problem.getProblemOrder(),
              problem.getName(),
              problem.getContent(),
              answer != null ? answer.getUserAnswer() : "",
              problem.getCorrectAnswer(),
              problem.getExplanation(),
              answer != null && Boolean.TRUE.equals(answer.getIsCorrect())
          );
        })
        .toList();

    return new MyExamDetailResponse(room.getId(), room.getName(), room.getDescription(), roomUser.getScore(), problems);
  }

  // 요청 유저가 현재 소속된 공간 ID를 조회 (소속 공간이 없으면 null)
  private UUID resolveCurrentSpaceId(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_USER_NOT_FOUND));
    return user.getSpace() != null ? user.getSpace().getId() : null;
  }

  private double calculateAccuracyRate(long solved, long attempted) {
    if (attempted == 0) {
      return 0.0;
    }
    return (double) solved / attempted * 100;
  }

  private OffsetDateTime startOfToday() {
    return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
  }

  private OffsetDateTime startOfThisWeek() {
    LocalDate mondayOfThisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    return mondayOfThisWeek.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
  }
}
