package com.momogo.core.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserProblemId implements Serializable {
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @Column(name = "problem_id", columnDefinition = "UUID")
    private UUID problemId;
}
