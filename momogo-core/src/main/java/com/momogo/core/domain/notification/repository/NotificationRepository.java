package com.momogo.core.domain.notification.repository;

import com.momogo.core.domain.notification.entity.Notification;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  //본인 알람 확인
  Optional<Notification> findByIdAndReceiverId(UUID id, UUID receiverId);

  //사용자의 전체 알림 개수 조회
  long countByReceiverId(UUID receiverId);

  //첫 페이지 조회를 위해, 사용자의 알림을 최신순으로 조회
  List<Notification> findByReceiverIdOrderByCreatedAtDescIdDesc(UUID receiverId, Pageable pageable);

  //커서 기반 다음 페이지 조회를 위해, 특정 시점(createdAt)과 ID 이후 알림을 최신순으로 조회
  @Query("""
      SELECT n FROM Notification n
      WHERE n.receiver.id = :receiverId
        AND (n.createdAt < :createdAt
             OR (n.createdAt = :createdAt AND n.id < :id))
      ORDER BY n.createdAt DESC, n.id DESC
      """)
  List<Notification> findNextPageByReceiverId(
      @Param("receiverId") UUID receiverId,
      @Param("createdAt") OffsetDateTime createdAt,
      @Param("id") UUID id,
      Pageable pageable
  );
}
