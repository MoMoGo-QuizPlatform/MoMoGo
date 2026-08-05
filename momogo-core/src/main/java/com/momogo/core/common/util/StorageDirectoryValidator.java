package com.momogo.core.common.util;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;

import java.util.ArrayList;
import java.util.List;

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

        // 윈도우 경로 구분자(\)를 표준 구분자(/)로 통일
        String normalized = directory.replace("\\", "/");
        // 슬래시(/) 기준으로 전체 경로 세그먼트 분할 (마지막 빈 세그먼트까지 포함)
        String[] segments = normalized.split("/", -1);

        List<String> cleanSegments = new ArrayList<>();
        for (String segment : segments) {
            // 상위 디렉터리 접근(Path Traversal) 세그먼트 차단
            if ("..".equals(segment)) {
                throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "허용되지 않는 저장 경로입니다.");
            }
            // 현재 디렉터리(.) 및 중복/시작/끝 슬래시로 인한 빈 세그먼트 제외
            if (!segment.isEmpty() && !".".equals(segment)) {
                cleanSegments.add(segment);
            }
        }
        return String.join("/", cleanSegments);
    }
}
