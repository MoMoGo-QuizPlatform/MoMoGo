package com.momogo.core.common.util;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;

/**
 * StorageService 구현체 간 directory 파라미터 처리 방식을 통일하기 위한 공통 검증기
 */
public final class StorageDirectoryValidator {

    private StorageDirectoryValidator() {
    }

    public static String validate(String directory) {
        if (directory == null || directory.isBlank()) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "저장 경로(directory)가 지정되지 않았습니다.");
        }
        // 정규화 후 ".." 세그먼트 포함 여부 검사 (경로 조작 차단)
        // 최상위 시스템 폴더로 접근하는 것을 방지
        String normalized = directory.replace("\\", "/");
        if (normalized.startsWith("/") || normalized.contains("..")) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "허용되지 않는 저장 경로입니다.");
        }
        // 앞뒤 슬래시 정리 (일관된 key/path 형식 보장)
        return normalized.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
