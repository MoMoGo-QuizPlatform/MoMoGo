package com.momogo.core.domain.room.mapper;

import com.momogo.core.domain.room.dto.response.RoomProblemExamResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.entity.RoomProblem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomProblemMapper {

  @Mapping(source = "room.id", target = "roomId")
  RoomProblemResponse toResponse(RoomProblem roomProblem);

  List<RoomProblemResponse> toResponseList(List<RoomProblem> roomProblems);

  @Mapping(source = "room.id", target = "roomId")
  RoomProblemExamResponse toExamResponse(RoomProblem roomProblem);

  List<RoomProblemExamResponse> toExamResponseList(List<RoomProblem> roomProblems);

}
