package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import com.momogo.core.domain.room.dto.request.RoomProblemCreatedRequest;
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
public class RoomProblem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "problem_order", nullable = false)
    private Integer problemOrder;

    @Column(name = "name")
    private String name;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    // 정적 팩토리 생성 메소드
    public static RoomProblem of(Room room, RoomProblemCreatedRequest request) {
        return RoomProblem.builder()
            .room(room)
            .problemOrder(request.problemOrder())
            .name(request.name())
            .content(request.content())
            .explanation(request.explanation())
            .correctAnswer(request.correctAnswer())
            .build();
    }
}
