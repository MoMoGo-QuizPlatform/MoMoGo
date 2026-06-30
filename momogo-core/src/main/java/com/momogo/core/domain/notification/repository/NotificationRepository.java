package com.momogo.core.domain.notification.repository;

import com.momogo.core.domain.notification.entity.Notification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  //본인 알람 확인
  Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);
}
