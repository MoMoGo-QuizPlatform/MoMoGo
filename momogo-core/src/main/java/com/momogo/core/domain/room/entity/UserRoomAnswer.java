package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import com.momogo.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "TBL_USER_ROOM_ANSWER")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoomAnswer extends BaseCreatedTimeEntity {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_problem_id", nullable = false)
    private RoomProblem roomProblem;

    @Column(name = "is_solved", nullable = false)
    private Boolean isSolved;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;
}
