package com.momogo.core.domain.problem.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import com.momogo.core.domain.space.entity.Space;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TBL_PROBLEM_CATEGORY")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemCategory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public static ProblemCategory create(
        Space space,
        String name) {

        return ProblemCategory.builder()
            .space(space)
            .name(name)
            .build();
    }

    public void update(
        String name) {

        if (name != null) this.name = name;
    }
}
