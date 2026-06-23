package com.momogo.core.domain.problem.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import com.momogo.core.domain.space.entity.Space;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "TBL_PROBLEM_CATEGORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemCategory extends BaseCreatedTimeEntity {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
