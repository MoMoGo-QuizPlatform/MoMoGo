package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.hibernate.query.SortDirection;

import java.util.UUID;

public record UserPageRequest(

        String nameLike,
        String emailLike,
        String cursor, // 가입일(OffsetDateTime) 문자열
        UUID idAfter,  // 보조 식별자 유저 ID

        @Min(value = 1, message = "페이지를 가져올 개수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "페이지를 가져올 개수는 100 이하이어야 합니다.")
        Integer limit,

        SortDirection sortDirection,

        @Pattern(
                regexp = "^(createdAt|updatedAt|deletedAt)$",
                message = "정렬 기준은 `createdAt`, `updatedAt`, `deletedAt` 만 지원합니다."
        )
        String sortBy
) {
    public UserPageRequest {
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (sortDirection == null) {
            sortDirection = SortDirection.DESCENDING;
        }
        // 파라미터가 전달되지 않았을 때만 기본값 20 적용
        if (limit == null) {
            limit = 20;
        }
    }
}
