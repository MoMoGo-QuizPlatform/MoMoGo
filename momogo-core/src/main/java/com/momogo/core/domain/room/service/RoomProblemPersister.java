package com.momogo.core.domain.room.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.problem.entity.ProblemCategory;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import com.momogo.core.domain.room.mapper.RoomProblemMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import com.momogo.core.domain.room.repository.RoomRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomProblemPersister {

  private final RoomRepository roomRepository;

  private final RoomProblemRepository roomProblemRepository;

  private final RoomProblemMapper roomProblemMapper;

  /**
   * 방 문제 저장 + 순번 채번만 수행 (LLM 호출 X)
   */
  @Transactional
  public List<RoomProblemResponse> saveGeneratedProblems(
      UUID roomId,
      ProblemCategory category,
      List<GeneratedProblemData> generated) {

    // 같은 방에 대한 동시 채번 요청을 직렬화하기 위해 비관적 락으로 재조회
    Room lockedRoom = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new BusinessException(RoomErrorCode.ROOM_NOT_FOUND));

    Integer nextOrder = roomProblemRepository.findMaxProblemOrderByRoomId(lockedRoom.getId())
        .map(o -> o + 1)
        .orElse(1);

    List<RoomProblem> roomProblemsToSave = new ArrayList<>();

    for (GeneratedProblemData dto : generated) {

      roomProblemsToSave.add(
          RoomProblem.of(
              lockedRoom,
              category,
              nextOrder++,
              dto.name(),
              dto.content(),
              dto.explanation(),
              dto.correctAnswer()));
    }

    List<RoomProblem> saved = roomProblemRepository.saveAll(roomProblemsToSave);

    return saved.stream()
        .map(roomProblemMapper::toResponse)
        .toList();
  }
}
