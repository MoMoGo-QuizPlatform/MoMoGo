package com.momogo.core.domain.room.mapper;

import com.momogo.core.domain.room.dto.response.RoomProblemExamResponse;
import com.momogo.core.domain.room.dto.response.RoomProblemResponse;
import com.momogo.core.domain.room.entity.RoomProblem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoomProblemMapper {

  @Mapping(source = "room.id", target = "roomId")
  RoomProblemResponse toResponse(RoomProblem roomProblem);

  @Mapping(source = "room.id", target = "roomId")
  RoomProblemExamResponse toExamResponse(RoomProblem roomProblem);
}
