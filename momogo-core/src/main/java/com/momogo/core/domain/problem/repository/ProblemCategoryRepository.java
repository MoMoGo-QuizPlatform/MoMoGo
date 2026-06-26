package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.ProblemCategory;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemCategoryRepository extends JpaRepository<ProblemCategory, UUID> {

  // 카테고리 이름 중복 체크
  boolean existsByName(String name);
}
