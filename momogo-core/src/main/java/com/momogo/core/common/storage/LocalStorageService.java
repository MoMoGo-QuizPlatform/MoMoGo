package com.momogo.core.common.storage;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.util.storage.ImageProcessor;
import com.momogo.core.common.util.storage.ImageResizeSpec;
import com.momogo.core.common.util.storage.StorageDirectoryValidator;
import com.momogo.core.common.util.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final ImageProcessor imageProcessor;
    private final Path uploadRoot;

    public LocalStorageService(
            ImageProcessor imageProcessor,
            @Value("${app.file.upload.dir:./uploads}") String uploadDir
    ) {
        this.imageProcessor = imageProcessor;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("[LocalStorageService] 파일 저장 절대 경로 지정 완료: {}", this.uploadRoot);
    }

    @Override
    public String upload(InputStream inputStream, String originalFileName, String contentType, String directory, ImageResizeSpec resizeSpec) {
        // try-with-resources 구문으로 원본 inputStream 및 validatedStream 자원 수명주기를 안전하게 관리
        try (InputStream src = inputStream) {
            // 검증 실패로 예외가 발생하여도 src(inputStream)가 자동으로 close() 되도록 보장한다.
            String safeDirectory = StorageDirectoryValidator.validate(directory);

            ImageProcessor.ImageValidationResult validationResult =
                    imageProcessor.validateImage(src, originalFileName, contentType);

            byte[] bytes = imageProcessor.resizeImage(
                    validationResult.data(),
                    validationResult.format(),
                    validationResult.width(),
                    validationResult.height(),
                    resizeSpec
            );

            try (InputStream validatedStream = new ByteArrayInputStream(bytes)) {
                String savedFileName = UUID.randomUUID() + validationResult.extension();

                // directory가 uploadRoot 바깥으로 빠져나가지 않는지 검증 (Path Traversal 방지)
                Path uploadPath = resolveSafely(uploadRoot, safeDirectory);
                Files.createDirectories(uploadPath);

                Path targetPath = resolveSafely(uploadPath, savedFileName);
                Files.copy(validatedStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

                return savedFileName;
            }
        } catch (BusinessException e) {
            // 검증 실패(잘못된 경로/확장자/해상도 등)
            log.warn("[LocalStorageService] 파일 업로드 검증 실패 - originalFileName: {}, directory: {}",
                    originalFileName, directory, e);
            throw e;
        } catch (IOException e) {
            // 디스크 I/O 등 시스템 오류
            log.error("[LocalStorageService] 파일 업로드 실패 - originalFileName: {}, directory: {}",
                    originalFileName, directory, e);
            throw new BusinessException(
                    GlobalErrorCode.FILE_UPLOAD_FAILED,
                    "파일 저장 중 시스템 오류가 발생했습니다.",
                    e.getMessage()
            );
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        // 외부 HTTP/HTTPS URL인 경우 로컬 디스크 삭제 대상이 아니므로 즉시 리턴
        if (UrlUtils.isExternalUrl(fileUrl)) {
            return;
        }

        String relativePath = fileUrl;
        if (fileUrl.contains("/uploads/")) {
            relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + "/uploads/".length());
        }

        try {
            Path filePath = resolveSafely(uploadRoot, relativePath);

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("[LocalStorageService] 물리 파일 삭제 완료: {}", filePath);
            } else {
                log.warn("[LocalStorageService] 삭제할 파일이 디스크에 존재하지 않습니다. {}", filePath);
            }
        } catch (BusinessException e) {
            log.warn("[LocalStorageService] 허용되지 않는 삭제 경로 요청 차단: {}", fileUrl);
        } catch (IOException e) {
            log.error("[LocalStorageService] 물리 파일 삭제 실패: {}", fileUrl, e);
        }
    }

    /**
     * base 경로 하위로 relative를 결합한 뒤 정규화하고 결과 경로가 base 바깥으로 벗어나지 않는지 검증합니다.
     * 절대 경로 위장( "../") 경로 조작을 모두 차단합니다.
     */
    private Path resolveSafely(Path base, String relative) {
        if (relative == null) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "유효하지 않은 경로입니다.");
        }
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) {
            log.warn("[LocalStorageService] 허용된 경로를 벗어난 요청 차단: base={}, relative={}", base, relative);
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "허용되지 않는 경로입니다.");
        }
        return resolved;
    }
}
