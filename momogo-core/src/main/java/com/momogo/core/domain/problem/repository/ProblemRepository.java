package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID> {
}
