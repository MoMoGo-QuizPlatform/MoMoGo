package com.momogo.core.domain.problem.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "TBL_PROBLEM_COUNTERS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemCounters extends BaseCreatedTimeEntity {
    @Id
    @Column(name = "problem_id", columnDefinition = "UUID")
    private UUID problemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Builder.Default
    @Column(name = "solved_count")
    private Integer solvedCount = 0;

    @Builder.Default
    @Column(name = "try_count")
    private Integer tryCount = 0;
}
