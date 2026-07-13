package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.room.exception.RoomErrorCode;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TBL_ROOM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
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

    @Builder.Default
    @Column(name = "is_ai_grading_in_progress")
    private Boolean isAiGradingInProgress = false;

    @Column(name = "test_start_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime testStartAt;

    @Column(name = "test_end_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime testEndAt;

    // 정적 팩토리 생성 메소드
    public static Room of(Space space, String name, String description, OffsetDateTime testStartAt, OffsetDateTime testEndAt) {

        // 엔티티 생성 불변식 검증
        if (testStartAt == null || testEndAt == null || !testStartAt.isBefore(testEndAt)) {
            throw new BusinessException(RoomErrorCode.INVALID_TEST_TIME);
        }

        return Room.builder()
            .space(space)
            .name(name)
            .description(description)
            .testStartAt(testStartAt)
            .testEndAt(testEndAt)
            .isEnded(false)
            .build();
    }

    // 마감 처리
    public void finalizeTest() {
        this.isEnded = true;
    }

    // AI 채점 진행 중 플래그 설정
    public void markAiGradingInProgress() {
        this.isAiGradingInProgress = true;
    }

    // AI 채점 진행 중 플래그 해제
    public void clearAiGradingInProgress() {
        this.isAiGradingInProgress = false;
    }
}
