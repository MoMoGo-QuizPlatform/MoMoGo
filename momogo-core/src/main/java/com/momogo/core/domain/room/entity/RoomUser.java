package com.momogo.core.domain.room.entity;

import com.momogo.core.common.base.BaseCreatedTimeEntity;
import com.momogo.core.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "TBL_ROOM_USER")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(RoomUserId.class)
public class RoomUser extends BaseCreatedTimeEntity {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "is_attended")
    private Boolean isAttended = false;

    @Builder.Default
    @Column(name = "score")
    private Integer score = 0;

    // 정적 팩토리 생성 메소드
    public static RoomUser of(Room room, User user) {
        return RoomUser.builder()
            .room(room)
            .user(user)
            .isAttended(false)
            .score(0)
            .build();
    }
}
