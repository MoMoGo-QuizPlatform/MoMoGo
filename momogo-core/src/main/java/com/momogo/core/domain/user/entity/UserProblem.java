package com.momogo.core.domain.user.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import com.momogo.core.domain.problem.entity.Problem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "TBL_USER_PROBLEM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProblem extends BaseCreatedTimeEntity {
    @EmbeddedId
    private UserProblemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("problemId")
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(name = "is_solved", nullable = false)
    private Boolean isSolved;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;
}
