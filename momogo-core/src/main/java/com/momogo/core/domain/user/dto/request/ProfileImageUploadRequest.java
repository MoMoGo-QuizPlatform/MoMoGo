package com.momogo.core.domain.user.dto.request;

import java.io.InputStream;

/**
 * 프로필 이미지 업로드를 위한 데이터 전송 객체(DTO)
 * 웹 계층(MultipartFile)과의 결합을 방지하고 core 모듈이 특정 프레임워크 스펙에 종속되지 않도록 합니다.
 */
public record ProfileImageUploadRequest(
        InputStream inputStream,
        String originalFilename,
        String contentType,
        long size
) {
}
