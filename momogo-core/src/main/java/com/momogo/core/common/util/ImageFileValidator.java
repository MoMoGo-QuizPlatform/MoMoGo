package com.momogo.core.common.util;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

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
     * @param inputStream      파일 데이터 스트림
     * @param originalFilename 원본 파일 이름
     * @param contentType      파일의 Content-Type
     * @return 검증 후 다시 처음부터 읽을 수 있도록 분리 및 복사된 InputStream
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

        // 3. 업로드 상한 내에서 바이트를 안전하게 복사 (OOM 방지 및 스트림 복제)
        byte[] fileBytes = readWithLimit(inputStream, maxMarkSize);

        try {
            // 4. 검증용 ByteArrayInputStream 생성하여 ImageIO 검증
            try (InputStream validationStream = new ByteArrayInputStream(fileBytes)) {
                if (ImageIO.read(validationStream) == null) {
                    throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "손상되었거나 변조된 이미지 파일입니다.");
                }
            }

            // 5. 저장용으로 사용할 새로운 ByteArrayInputStream 반환
            return new ByteArrayInputStream(fileBytes);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "이미지 분석 중 오류가 발생했습니다.");
        }
    }

    /**
     * 지정한 바이트 크기 상한(limit) 내에서만 스트림을 읽어 바이트 배열로 반환합니다.
     * 상한을 초과할 경우 즉시 예외를 발생시켜 OOM을 예방합니다.
     */
    private byte[] readWithLimit(InputStream inputStream, int limit) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytes = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                if (totalBytes > limit) {
                    throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "업로드 가능한 최대 파일 크기를 초과했습니다.");
                }
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "파일을 읽는 중 오류가 발생했습니다.");
        }
    }
}
