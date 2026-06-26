package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import com.momogo.core.domain.space.entity.Space;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "TBL_ROOM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseTimeEntity {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "is_ended")
    private Boolean isEnded = false;

    @Column(name = "test_start_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime testStartAt;

    @Column(name = "test_end_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime testEndAt;
}
