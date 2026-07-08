package com.momogo.core.domain.room.service;

import com.momogo.core.domain.problem.dto.response.GeneratedProblemData;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.entity.Room;
import com.momogo.core.domain.room.entity.RoomProblem;
import com.momogo.core.domain.room.mapper.RoomProblemMapper;
import com.momogo.core.domain.room.repository.RoomProblemRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomProblemPersister {

  private final RoomProblemRepository roomProblemRepository;

  private final RoomProblemMapper roomProblemMapper;

  /**
   * 방 문제 저장 + 순번 채번만 수행 (LLM 호출 X)
   */
  @Transactional
  public List<RoomProblemResponse> saveGeneratedProblems(
      Room room,
      List<GeneratedProblemData> generated) {

    Integer nextOrder = roomProblemRepository.findMaxProblemOrderByRoomId(room.getId())
        .map(o -> o + 1)
        .orElse(1);

    List<RoomProblem> saved = new ArrayList<>();

    for (GeneratedProblemData dto : generated) {

      saved.add(roomProblemRepository.save(
          RoomProblem.of(
              room,
              nextOrder++,
              dto.name(),
              dto.content(),
              dto.explanation(),
              dto.correctAnswer())));
    }

    return saved.stream()
        .map(roomProblemMapper::toResponse)
        .toList();
  }
}
