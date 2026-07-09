package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.Room;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

  @Query("select r from Room r join fetch r.space where r.id = :roomId")
  Optional<Room> findByIdWithSpace(@Param("roomId") UUID roomId);

  // 방 문제 채번 구간 직렬화용 비관적 락 조회 (동시 생성 요청의 순번 중복 방지)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :roomId")
  Optional<Room> findByIdForUpdate(@Param("roomId") UUID roomId);
}
