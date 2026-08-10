package com.momogo.core.common.util.storage;

/**
 * 이미지 리사이징 규격(목표 가로/세로 픽셀 크기)을 나타내는 값 객체.
 * 도메인별로 필요한 리사이징 규격을 상수로 미리 정의해두고 재사용합니다.
 */
public record ImageResizeSpec(int width, int height) {

    public ImageResizeSpec {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width, height는 0보다 커야 합니다.");
        }
    }

    /**
     * 프로필 이미지 규격 (정사각형 300x300, 중앙 크롭)
     */
    public static final ImageResizeSpec PROFILE = new ImageResizeSpec(300, 300);
}
