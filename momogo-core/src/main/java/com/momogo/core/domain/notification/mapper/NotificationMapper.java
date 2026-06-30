package com.momogo.core.domain.notification.mapper;

import com.momogo.core.domain.notification.dto.NotificationDto;
import com.momogo.core.domain.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  NotificationDto toDto(Notification notification);
}