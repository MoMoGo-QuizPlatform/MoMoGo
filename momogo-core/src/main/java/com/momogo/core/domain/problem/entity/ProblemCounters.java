package com.momogo.core.domain.problem.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "solved_count", nullable = false)
    private Integer solvedCount;

    @Column(name = "try_count", nullable = false)
    private Integer tryCount;

    // 문제 제출 시 호출
    public void recordAttempt(boolean isSolved) {

        this.tryCount++;

        if (isSolved) {
            this.solvedCount++;
        }
    }

    // 문제 생성 시 문제 카운트를 같이 만들어주는 팩토리 메서드
    public static ProblemCounters init(Problem problem) {

        return ProblemCounters.builder()
            .problem(problem)
            .solvedCount(0)
            .tryCount(0)
            .build();
    }
}
