package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.Room;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

  @Query("select r from Room r join fetch r.space where r.id = :roomId")
  Optional<Room> findByIdWithSpace(@Param("roomId") UUID roomId);
}
