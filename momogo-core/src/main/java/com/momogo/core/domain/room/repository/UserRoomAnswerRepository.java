package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.UserRoomAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoomAnswerRepository extends JpaRepository<UserRoomAnswer, UUID>  {

  @Query("select u from UserRoomAnswer u join fetch u.roomProblem rp where rp.room.id = :roomId")
  List<UserRoomAnswer> findByRoomProblemRoomId(@Param("roomId") java.util.UUID roomId);

  @Query("select u from UserRoomAnswer u join fetch u.roomProblem rp where rp.room.id = :roomId and u.user.id = :userId")
  List<UserRoomAnswer> findByRoomProblemRoomIdAndUserId(@Param("roomId") java.util.UUID roomId, @Param("userId") java.util.UUID userId);
}
