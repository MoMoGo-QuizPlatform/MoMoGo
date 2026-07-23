package com.momogo.core.domain.user.repository;

import com.momogo.core.domain.user.entity.UserProblem;
import com.momogo.core.domain.user.entity.UserProblemId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProblemRepository extends JpaRepository<UserProblem, UserProblemId> {

    // 대시보드 통계는 항상 "내가 지금 속한 공간"의 문제 풀이만 집계해야 한다.
    // (공간을 나갔다가 다른 공간에 가입해도 예전 UserProblem 행은 남아있으므로,
    //  problem.space로 반드시 필터링해서 다른 공간의 이력이 섞여 보이지 않게 한다.)
    long countByUser_IdAndProblem_Space_Id(UUID userId, UUID spaceId);

    long countByUser_IdAndProblem_Space_IdAndIsSolvedTrue(UUID userId, UUID spaceId);

    @Query("select count(up) from UserProblem up where up.user.id = :userId and up.problem.space.id = :spaceId and up.createdAt >= :start and up.createdAt < :end")
    long countAttemptedByUserAndSpaceAndPeriod(@Param("userId") UUID userId, @Param("spaceId") UUID spaceId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @Query("select count(up) from UserProblem up where up.user.id = :userId and up.problem.space.id = :spaceId and up.isSolved = true and up.createdAt >= :start and up.createdAt < :end")
    long countSolvedByUserAndSpaceAndPeriod(@Param("userId") UUID userId, @Param("spaceId") UUID spaceId, @Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    @EntityGraph(attributePaths = {"problem", "problem.category"})
    List<UserProblem> findAllByUser_IdAndProblem_Space_IdOrderByCreatedAtDesc(UUID userId, UUID spaceId);
}
