package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.RoomUser;
import com.momogo.core.domain.room.entity.RoomUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomUserRepository extends JpaRepository<RoomUser, RoomUserId> {

}
