package com.momogo.core.domain.user.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "TBL_USER")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "email",
            nullable = false
    )
    private String email;

    @Column(
            name = "profile_image_url",
            length = 500
    )
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "role",
            nullable = false,
            length = 50
    )
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "social",
            length = 50
    )
    private SocialType social;

    @Builder.Default
    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Column(
            name = "deleted_at",
            columnDefinition = "TIMESTAMPTZ"
    )
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    public User(String name, String email, String password, SocialType social) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = UserRole.USER;
        this.social = social;
        this.isBanned = false;
    }

    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public void updatePassword(String encodedPassword) {
        if (encodedPassword != null && !encodedPassword.isBlank()) {
            this.password = encodedPassword;
        }
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void ban() {
        this.isBanned = true;
    }

    public void unban() {
        this.isBanned = false;
    }

    public void delete() {
        this.deletedAt = OffsetDateTime.now();
    }

    public void joinSpace(Space space, UserRole role) {
        this.space = space;
        this.role = role;
    }

    public void leaveSpace() {
        this.space = null;
        this.role = UserRole.USER;
    }
}
