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
@Table(name = "TBL_PROBLEM")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProblemCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "correct_answer", columnDefinition = "TEXT")
    private String correctAnswer;

    public static Problem create(
        ProblemCategory category,
        Space space,
        String name,
        String content,
        String explanation,
        String correctAnswer) {

        return Problem.builder()
            .category(category)
            .space(space)
            .name(name)
            .content(content)
            .explanation(explanation)
            .correctAnswer(correctAnswer)
            .build();
    }

    public void update(
        String name,
        String content,
        String explanation,
        String correctAnswer) {

        if (name != null) this.name = name;

        if (content != null) this.content = content;

        if (explanation != null) this.explanation = explanation;

        if (correctAnswer != null) this.correctAnswer = correctAnswer;
    }
}
