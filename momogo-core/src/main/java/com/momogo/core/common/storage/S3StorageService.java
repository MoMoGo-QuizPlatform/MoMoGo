package com.momogo.core.common.storage;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.util.ImageFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
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
            @Value("${app.aws.region:ap-northeast-2}") String region
    ) {
        this.imageFileValidator = imageFileValidator;
        this.bucket = bucket;
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
        log.info("[S3StorageService] 버킷 지정 완료: {}", bucket);
    }

    @Override
    public String upload(InputStream inputStream, String originalFileName, String contentType, String directory) {
        InputStream validatedStream = imageFileValidator.validateImage(inputStream, originalFileName, contentType);

        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String savedFileName = UUID.randomUUID() + extension;
        String key = directory + "/" + savedFileName;

        try {
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
        } catch (IOException e) {
            log.error("[S3StorageService] 파일 업로드 실패 - originalFileName: {}, directory: {}", originalFileName, directory, e);
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

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileUrl)
                        .build()
        );
        log.info("[S3StorageService] 객체 삭제 완료: {}", fileUrl);
    }
}
