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

  // "참여 완료 시험" 집계도 항상 "내가 지금 속한 공간"의 시험만 대상으로 해야 한다.
  // (공간을 나갔다가 다른 공간에 가입해도 예전 RoomUser 행은 남아있으므로,
  //  room.space로 반드시 필터링해서 다른 공간의 이력이 섞여 보이지 않게 한다.)
  long countByUser_IdAndIsAttendedTrueAndRoom_Space_Id(UUID userId, UUID spaceId);

  @EntityGraph(attributePaths = {"room"})
  List<RoomUser> findAllByUser_IdAndRoom_Space_Id(UUID userId, UUID spaceId);
}
