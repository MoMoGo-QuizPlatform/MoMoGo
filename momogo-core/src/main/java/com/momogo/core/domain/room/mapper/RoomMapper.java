package com.momogo.core.domain.room.mapper;

import com.momogo.core.domain.room.dto.response.RoomResponse;
import com.momogo.core.domain.room.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoomMapper {

  @Mapping(target = "spaceId", source = "space.id")
  RoomResponse toResponse(Room room);
}
