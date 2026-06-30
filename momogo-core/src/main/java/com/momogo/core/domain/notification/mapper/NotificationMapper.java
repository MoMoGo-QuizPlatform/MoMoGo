package com.momogo.core.domain.notification.mapper;

import com.momogo.core.domain.notification.dto.response.NotificationResponse;
import com.momogo.core.domain.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(source = "isConfirmed", target = "isConfirmed")
  NotificationResponse toResponse(Notification notification);
}