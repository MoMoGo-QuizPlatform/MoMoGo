package com.momogo.core.common.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity extends BaseCreatedTimeEntity {
    @LastModifiedDate
    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
}
