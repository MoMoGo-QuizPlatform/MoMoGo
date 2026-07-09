package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomUserRepository extends JpaRepository<RoomUser, RoomUserId> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"user"})
  @Query("select ru from RoomUser ru where ru.id = :id")
  Optional<RoomUser> findByIdForUpdate(@Param("id") RoomUserId id);

  @EntityGraph(attributePaths = {"user"})
  List<RoomUser> findAllByRoomId(UUID roomId);
}
