package com.momogo.core.domain.user.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import com.momogo.core.common.security.PasswordEncryptor;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
            nullable = false,
            length = 50
    )
    private SocialType social;

    @Builder.Default
    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned = false;

    @Column(name = "temp_password")
    private String tempPassword;

    @Column(name = "temp_password_expired_at")
    private OffsetDateTime tempPasswordExpiredAt;

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

    // 임시 비밀번호를 세팅하고 만료 시간을 현재 시간 기준으로 3분 뒤로 설정합니다.
    public void setTemporaryPassword(String hashedTempPassword, OffsetDateTime currentTime) {
        this.tempPassword = hashedTempPassword;
        this.tempPasswordExpiredAt = currentTime.plusMinutes(3);
    }

    // 비밀번호 변경 시 임시 비밀번호를 초기화 합니다.
    public void clearTemporaryPassword() {
        this.tempPassword = null;
        this.tempPasswordExpiredAt = null;
    }

    public boolean isTemporaryPasswordActive(OffsetDateTime currentTime) {
        return tempPassword != null &&
                tempPasswordExpiredAt != null &&
                currentTime.isBefore(tempPasswordExpiredAt);
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

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isRestorable() {
        if (this.deletedAt == null) {
            return false;
        }
        return this.deletedAt.plusDays(30).isAfter(OffsetDateTime.now());
    }

    public void joinSpace(Space space, UserRole role) {
        this.space = space;
        this.role = role;
    }

    public void leaveSpace() {
        this.space = null;
        this.role = UserRole.USER;
    }

    public void changeRole(UserRole role) {
        if (role != null) {
            this.role = role;
        }
    }
}
