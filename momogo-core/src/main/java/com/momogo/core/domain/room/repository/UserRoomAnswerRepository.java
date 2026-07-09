package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.UserRoomAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoomAnswerRepository extends JpaRepository<UserRoomAnswer, UUID>  {

  List<UserRoomAnswer> findByRoomProblemRoomId(UUID roomId);
}
