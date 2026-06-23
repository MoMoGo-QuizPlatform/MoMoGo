package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import com.momogo.core.domain.problem.entity.Problem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "TBL_ROOM_PROBLEM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomProblem extends BaseCreatedTimeEntity {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "problem_order", nullable = false)
    private Integer problemOrder;
}
