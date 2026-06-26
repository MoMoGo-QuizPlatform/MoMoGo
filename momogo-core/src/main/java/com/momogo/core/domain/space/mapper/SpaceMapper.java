package com.momogo.core.domain.space.mapper;

import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.dto.response.SpaceResponse;
import com.momogo.core.domain.space.entity.Space;
import java.util.List;
import java.util.UUID;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SpaceMapper {

  // 1. Entity -> Response Dto
  SpaceResponse toResponse(Space space);

  // 1-2. Entity List -> Response Dto List
  List<SpaceResponse> toResponseList(List<Space> spaces);

  // 2. CreateRequest -> Entity
  @Mapping(target = "id", source = "id")
  Space toEntity(SpaceCreateRequest request, UUID id);

  // 3. UpdatedRequest -> Entity
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE) // null 값 덮어쓰기 금지(기존 값 유지) 처리
  @Mapping(target = "id", ignore = true)
  void updateFromDto(SpaceUpdateRequest request, @MappingTarget Space space);
}
