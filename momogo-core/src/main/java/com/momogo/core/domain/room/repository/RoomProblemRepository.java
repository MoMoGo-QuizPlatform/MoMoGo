package com.momogo.core.domain.room.repository;

import com.momogo.core.domain.room.entity.RoomProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RoomProblemRepository extends JpaRepository<RoomProblem, UUID>, RoomProblemRepositoryCustom {
}
