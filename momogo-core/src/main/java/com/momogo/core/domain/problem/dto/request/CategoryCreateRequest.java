package com.momogo.core.domain.problem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 카테고리 생성 요청 DTO
 */
public record CategoryCreateRequest(

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    @Size(max = 100, message = "카테고리 이름은 100자 이하여야 합니다.")
    String name
) {}
