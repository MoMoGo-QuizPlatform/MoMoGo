package com.momogo.core.domain.space.entity;

import com.momogo.core.common.base.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.UUID;

@Entity
@Table(name = "TBL_SPACE")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space extends BaseTimeEntity {
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "space_image_url", length = 500)
    private String spaceImageUrl;

    @Column(name = "space_password")
    private String spacePassword;

    // 더티 체크
    public void updateSpaceInfo(String name, String description, String spacePassword, String spaceImageUrl) {
        this.name = name;
        this.description = description;
        if (spacePassword != null && !spacePassword.isBlank()) {
            this.spacePassword = spacePassword;
        }
        this.spaceImageUrl = spaceImageUrl;
    }

    public void updateSpacePassword(String encryptedPassword) {
        if (encryptedPassword != null && !encryptedPassword.isBlank()) {
            this.spacePassword = encryptedPassword;
        }
    }
}
