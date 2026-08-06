package com.momogo.core.common.storage;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.util.ImageFileValidator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final ImageFileValidator imageFileValidator;
    private final S3Client s3Client;
    private final String bucket;

    public S3StorageService(
            ImageFileValidator imageFileValidator,
            @Value("${app.aws.s3-bucket}") String bucket,
            @Value("${app.aws.region}") String region
    ) {
        this.imageFileValidator = imageFileValidator;
        this.bucket = bucket;
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
        log.info("[S3StorageService] 버킷 지정 완료: {}", bucket);
    }

    @Override
    public String upload(InputStream inputStream, String originalFileName, String contentType, String directory) {
        // try-with-resources 구문으로 원본 inputStream 및 validatedStream 자원 수명주기를 안전하게 관리
        try (InputStream src = inputStream;
             InputStream validatedStream = imageFileValidator.validateImage(src, originalFileName, contentType)) {

            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String savedFileName = UUID.randomUUID() + extension;
            String key = directory + "/" + savedFileName;

            byte[] bytes = validatedStream.readAllBytes();
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
            return savedFileName;

        } catch (IOException | SdkException e) {
            log.error("[S3StorageService] S3 파일 업로드 실패 - originalFileName: {}, directory: {}", originalFileName, directory, e);
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

        String key = parseS3Key(fileUrl);
        if (key == null) {
            return;
        }

        executeDeleteWithRetry(key, fileUrl);
    }

    @PreDestroy
    public void shutdown() {
        if (s3Client != null) {
            try {
                s3Client.close();
                log.info("[S3StorageService] S3Client 자원 해제 완료");
            } catch (Exception e) {
                log.warn("[S3StorageService] S3Client 종료 중 예외 발생", e);
            }
        }
    }

    // URL 주소에서 순수 S3 Object Key를 추출합니다. (URI.getPath()가 이미 percent-decoding을 수행하므로 URLDecoder 이중 호출 방지)
    private String parseS3Key(String fileUrl) {
        if (!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")) {
            return fileUrl;
        }
        try {
            String path = URI.create(fileUrl).getPath();
            if (path == null) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (Exception e) {
            log.warn("[S3StorageService] 올바르지 않은 URL 형식: {}", fileUrl);
            return null;
        }
    }

    // S3 객체 삭제 시 지수 백오프(100ms -> 200ms)로 총 3회 시도(재시도 2회)하며, NoSuchKey(404) 외의 실패는 예외를 발생시킵니다.
    private void executeDeleteWithRetry(String key, String originalUrl) {
        int maxAttempts = 3;
        long backoffMs = 100;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                s3Client.deleteObject(
                        DeleteObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build()
                );
                log.info("[S3StorageService] 객체 삭제 완료: {}", key);
                return;
            } catch (S3Exception e) {
                // 404 (NoSuchKey)는 이미 삭제된 상태이므로 멱등성 성공으로 간주하고 정상 반환
                if (e.statusCode() == 404) {
                    log.info("[S3StorageService] S3 객체가 이미 존재하지 않음 (NoSuchKey): key={}", key);
                    return;
                }
                // 그 외 4xx (403 AccessDenied 등)는 재시도 불가하므로 즉시 예외 발생
                if (e.statusCode() >= 400 && e.statusCode() < 500) {
                    log.error("[S3StorageService] S3 객체 삭제 거부/오류 (HTTP {}): key={}", e.statusCode(), key, e);
                    throw new BusinessException(
                            GlobalErrorCode.INTERNAL_SERVER_ERROR,
                            "S3 객체 삭제에 실패했습니다 (HTTP " + e.statusCode() + ")",
                            e.getMessage()
                    );
                }
                handleRetryFailure(attempt, maxAttempts, key, originalUrl, backoffMs, e);
            } catch (SdkException e) {
                handleRetryFailure(attempt, maxAttempts, key, originalUrl, backoffMs, e);
            }
            backoffMs *= 2; // 지수 백오프 (100 -> 200)
        }
    }

    private void handleRetryFailure(int attempt, int maxAttempts, String key, String originalUrl, long backoffMs, Exception e) {
        if (attempt == maxAttempts) {
            log.error("[S3StorageService] S3 객체 삭제 최종 실패 ({}회 시도): {}", maxAttempts, originalUrl, e);
            throw new BusinessException(
                    GlobalErrorCode.INTERNAL_SERVER_ERROR,
                    "S3 객체 삭제가 최종 실패했습니다.",
                    e.getMessage()
            );
        } else {
            log.warn("[S3StorageService] S3 객체 삭제 재시도 ({}/{}): {}", attempt, maxAttempts, key);
            try {
                Thread.sleep(backoffMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("[S3StorageService] S3 재시도 대기 중 인터럽트 발생 - 루프 중단: {}", key);
                throw new BusinessException(
                        GlobalErrorCode.INTERNAL_SERVER_ERROR,
                        "S3 객체 삭제 중 인터럽트가 발생했습니다.",
                        ie.getMessage()
                );
            }
        }
    }
}
