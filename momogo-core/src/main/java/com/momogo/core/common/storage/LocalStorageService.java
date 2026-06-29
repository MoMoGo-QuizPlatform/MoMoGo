package com.momogo.core.common.storage;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.util.ImageFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final ImageFileValidator imageFileValidator;

    @Override
    public String upload(InputStream inputStream, String originalFileName, String contentType, String directory) {
        // 이미지 유효성 정밀 검증 및 스트림 초기화
        InputStream validatedStream = imageFileValidator.validateImage(inputStream, originalFileName, contentType);

        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String savedFileName = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get("./uploads", directory);
        try {
            Files.createDirectories(uploadPath);
            Path targetPath = uploadPath.resolve(savedFileName);
            Files.copy(validatedStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            return savedFileName;
        } catch (IOException e) {
            log.error("[StorageService] 파일 업로드 실패 - originalFileName: {}, directory: {}", originalFileName, directory, e);
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

        String relativePath = fileUrl;
        if (fileUrl.contains("/uploads/")) {
            relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + "/uploads/".length());
        }

        try {
            Path filePath = Paths.get("./uploads").resolve(relativePath);

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("[StorageService] 물리 파일 삭제 완료: {}", filePath.toAbsolutePath());
            } else {
                log.warn("[StorageService] 삭제할 파일이 디스크에 존재하지 않습니다. {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("[StorageService] 물리 파일 삭제 실패: {}", fileUrl, e);
        }
    }
}
