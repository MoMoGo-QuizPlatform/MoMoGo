package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.ProblemCounters;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemCountersRepository extends JpaRepository<ProblemCounters, UUID> {

    // 동시 제출 시 Lost Update 방지용 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ProblemCounters c where c.problemId = :problemId")
    Optional<ProblemCounters> findByIdWithLock(@Param("problemId") UUID problemId);
}
