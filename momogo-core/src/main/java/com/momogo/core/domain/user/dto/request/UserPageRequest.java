package com.momogo.core.domain.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.query.SortDirection;

import java.util.UUID;

public record UserPageRequest(

        String nameLike,
        String emailLike,
        String cursor, // 가입일(OffsetDateTime) 문자열
        UUID idAfter,  // 보조 식별자 유저 ID

        @Min(value = 1, message = "페이지를 가져올 개수는 1 이상이어야 합니다.")
        Integer limit,

        @NotNull(message = "정렬 방향은 필수값입니다.")
        SortDirection sortDirection,

        @NotNull(message = "정렬 기준은 필수값입니다.")
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
        // 파라미터에 값이 전달되지 않을 경우 내부 에러가 발생하므로 이를 대비하여 기본 값을 20으로 설정
        if (limit == null || limit <= 0) {
            limit = 20;
        }
    }
}
