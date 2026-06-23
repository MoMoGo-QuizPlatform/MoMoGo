package com.momogo.core.domain.user.repository;

import com.momogo.core.domain.user.entity.UserProblem;
import com.momogo.core.domain.user.entity.UserProblemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProblemRepository extends JpaRepository<UserProblem, UserProblemId> {
}
