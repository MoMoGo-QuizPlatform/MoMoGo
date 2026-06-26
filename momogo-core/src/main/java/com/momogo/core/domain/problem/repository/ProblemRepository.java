package com.momogo.core.domain.problem.repository;

import com.momogo.core.domain.problem.entity.Problem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, UUID>, ProblemRepositoryCustom {

  /**
   * 문제 단건 조회
   * - 문제 수정, 삭제 제출 시에 문제 정보가 필요하니
   *   문제 하나를 카테고리와 함께 가져온다.
   */
  @Query("SELECT p FROM Problem p JOIN FETCH p.category WHERE p.id = :id")
  Optional<Problem> findByIdWithCategory(@Param("id") UUID id);

  /**
   * 카테고리 삭제 전 확인 메서드
   * - 카테고리 삭제 전 관련되어 있는 문제가 남아 있으면
   *   안되기에 미리 확인하는 용도
   */
  boolean existsByCategory_Id(UUID categoryId);
}
