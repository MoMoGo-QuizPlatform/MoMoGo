package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.ProblemCounters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProblemCountersRepository extends JpaRepository<ProblemCounters, UUID> {
}
