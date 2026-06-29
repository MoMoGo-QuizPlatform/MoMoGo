package com.momogo.core.common.util;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.springframework.util.unit.DataSize;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;

/**
 * 프로필 이미지 등 파일 업로드 시 업로드될 파일의 유효성과 보안성을 검증하는 컴포넌트입니다.
 * 확장자 및 MIME 타입을 검증하며, ImageIO를 사용하여 실제 이미지 파일 구조인지 검증합니다.
 */
@Component
public class ImageFileValidator {

    private final Set<String> allowedExtensions;
    private final Set<String> allowedMimeTypes;
    private final int maxMarkSize;

    public ImageFileValidator(
            @Value("${app.file.upload.allowed-extensions}") List<String> allowedExtensions,
            @Value("${app.file.upload.allowed-mime-types}") List<String> allowedMimeTypes,
            @Value("${app.file.upload.max-mark-size:10MB}") DataSize maxMarkSize
    ) {
        this.allowedExtensions = Set.copyOf(allowedExtensions);
        this.allowedMimeTypes = Set.copyOf(allowedMimeTypes);
        this.maxMarkSize = (int) maxMarkSize.toBytes();
    }

    /**
     * 파일 업로드 시 확장자, MIME 타입 및 실제 이미지 바이트 무결성을 일괄 검증합니다.
     *
     * @param inputStream 파일 데이터 스트림
     * @param originalFilename 원본 파일 이름
     * @param contentType 파일의 Content-Type
     * @return 검증 후 다시 처음부터 읽을 수 있도록 reset 처리된 InputStream
     */
    public InputStream validateImage(InputStream inputStream, String originalFilename, String contentType) {
        // 1. 파일 이름 및 확장자 검사
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "올바르지 않은 파일명입니다.");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!allowedExtensions.contains(ext)) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "허용되지 않는 파일 확장자입니다.");
        }

        // 2. MIME 타입 검증
        if (contentType == null || !allowedMimeTypes.contains(contentType.toLowerCase())) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "허용되지 않는 파일 타입(MIME)입니다.");
        }

        // 3. 실제 이미지 바이너리 디코딩 검증 (mark/reset 지원하도록 스트림 가공)
        InputStream markableStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);

        try {
            // 설정된 크기만큼 mark 지원하도록 설정
            markableStream.mark(maxMarkSize);

            // ImageIO로 실제로 읽어서 가짜 이미지 파일인지 판별
            if (ImageIO.read(markableStream) == null) {
                throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "손상되었거나 변조된 이미지 파일입니다.");
            }

            // 검증이 끝나면 반드시 스트림을 처음 위치로 원복
            markableStream.reset();
            return markableStream;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "이미지 분석 중 오류가 발생했습니다.");
        }
    }
}
