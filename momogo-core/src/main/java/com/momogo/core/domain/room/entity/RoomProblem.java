package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseTimeEntity;
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
@Table(name = "TBL_ROOM_PROBLEM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomProblem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
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

    // 방 전용 문제 생성 시 사용하는 팩토리 메서드
    public static RoomProblem create(
        Room room,
        Integer problemOrder,
        String name,
        String content,
        String explanation,
        String correctAnswer) {

        return RoomProblem.builder()
            .room(room)
            .problemOrder(problemOrder)
            .name(name)
            .content(content)
            .explanation(explanation)
            .correctAnswer(correctAnswer)
            .build();
    }

    // 방 문제 수정 시 사용하는 팩토리 메서드
    public void update(
        Integer problemOrder,
        String name,
        String content,
        String explanation,
        String correctAnswer) {

        if (problemOrder != null) this.problemOrder = problemOrder;

        if (name != null) this.name = name;

        if (content != null) this.content = content;

        if (explanation != null) this.explanation = explanation;

        if (correctAnswer != null) this.correctAnswer = correctAnswer;
    }
}
