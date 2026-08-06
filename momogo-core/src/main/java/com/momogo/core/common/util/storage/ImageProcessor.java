package com.momogo.core.common.util.storage;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 업로드될 파일의 유효성과 보안성을 검증하고, 필요 시 지정된 규격으로
 * 리사이징/압축을 수행하는 범용 이미지 처리 컴포넌트
 * 확장자를 1차 검증하고, 실제 바이트(매직바이트)를 기반으로 ImageIO가 인식하는 실제 이미지 포맷을 감지하여
 * 확장자와 일치하는지 교차 검증합니다. 클라이언트가 전달한 Content-Type 헤더는 신뢰하지 않습니다.
 * ImageResizeSpec으로 리사이징 규격을 전달받아, 프로필 이미지 전용 규격 등 특정 도메인에 종속되지 않습니다.
 */
@Slf4j
@Component
public class ImageProcessor {

    private static final double OUTPUT_QUALITY = 0.85;

    // 픽셀 폭탄(decompression bomb) 방어용 최대 허용 픽셀 수 (예: 4000 x 4000 => 1600만 화소)
    // 최대 1600만 * 4bytes 디코딩 기준 최대 약 61MB 메모리 사용
    private static final long MAX_PIXEL_COUNT = 4000L * 4000L;

    // 파일 확장자를 ImageIO 표준 포맷명으로 변환하여, 이미지 소스 분석 결과와 대조하기 위한 매핑 테이블
    private static final Map<String, String> EXTENSION_TO_FORMAT = Map.of(
            "jpg", "jpeg",
            "jpeg", "jpeg",
            "png", "png",
            "webp", "webp"
    );

    // 감지된 이미지 포맷을 웹 표준 HTTP Content-Type (MIME Type)으로 변환하는 매핑 테이블
    private static final Map<String, String> FORMAT_TO_MIME_TYPE = Map.of(
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    private final Set<String> allowedExtensions;
    private final int maxFileSize;

    public ImageProcessor(
            @Value("${app.file.upload.allowed-extensions}") List<String> allowedExtensions,
            @Value("${app.file.upload.max-file-size:10MB}") DataSize maxFileSize
    ) {
        this.allowedExtensions = allowedExtensions.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
        this.maxFileSize = (int) maxFileSize.toBytes();
    }

    /**
     * 검증이 완료되면 완료된 원본 이미지 바이트, 감지된 ContentType/포맷, 확장자,
     * 원본 가로/세로 픽셀 크기를 담는 결과 객체
     *
     * @param inputStream      파일 데이터 스트림
     * @param originalFilename 원본 파일 이름
     * @param contentType      클라이언트가 전달한 Content-Type (참고용, 신뢰하지 않음)
     * @return 검증된 스트림과 실제 감지된 Content-Type을 담은 결과 객체
     */
    public ImageValidationResult validateImage(InputStream inputStream, String originalFilename, String contentType) {
        // 1. 파일 이름 및 확장자 검사
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "올바르지 않은 파일명입니다.");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!allowedExtensions.contains(ext) || !EXTENSION_TO_FORMAT.containsKey(ext)) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "허용되지 않는 파일 확장자입니다.");
        }

        // 2. 업로드 상한 내에서 바이트를 안전하게 복사 (OOM 방지 및 스트림 복제)
        byte[] fileBytes = readWithLimit(inputStream, maxFileSize);

        // 3. 실제 바이트 기반 이미지 포맷 감지 및 검증 (contentType 파라미터는 사용하지 않음(변조 방지))
        ImageDimension dimension = detectAndValidateFormat(fileBytes);

        // 4. 확장자와 실제 감지된 포맷이 일치하는지 교차 검증 (위장 확장자 차단)
        String expectedFormat = EXTENSION_TO_FORMAT.get(ext);
        if (!expectedFormat.equals(dimension.format())) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_INPUT,
                    "파일 내용이 확장자와 일치하지 않습니다.",
                    "ext=" + ext + ", detectedFormat=" + dimension.format()
            );
        }

        String detectedMimeType = FORMAT_TO_MIME_TYPE.get(dimension.format());
        return new ImageValidationResult(
                fileBytes,
                detectedMimeType,
                "." + ext,
                dimension.format(),
                dimension.width(),
                dimension.height()
        );
    }

    /**
     * 검증 완료된 이미지를 지정된 규격으로 정사각형 중앙 크롭 라사이징 및 압축합니다.
     * 원본이 목표 규격보다 작은 경우, 원본의 짧은 변을 기준으로 리사이징하여 
     * 이미지가 억지로 확대되지 않도록 합니다.
     * 
     * @param originalBytes 검증이 완료된 원본 이미지 바이트
     * @param format 감지된 이미지 포맷
     * @param originalWidth 원본 이미지 가로 픽셀 크기
     * @param originalHeight 원본 이미지 세로 픽셀 크기
     * @param spec 목표 리사이징 규격
     * @return 리사이징 및 압축이 완료된 이미지 바이트 
     */
    public byte[] resizeImage(byte[] originalBytes, String format, int originalWidth, int originalHeight, ImageResizeSpec spec) {

        // 원본의 짧은 변과 목표 규격 중 더 작은 값을 정사각형 한 변으로 사용
        int targetSize = Math.min(Math.min(originalWidth, originalHeight), Math.min(spec.width(), spec.height()));

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .size(targetSize, targetSize)
                    .crop(Positions.CENTER)
                    .outputQuality(OUTPUT_QUALITY) // 85% 품질 압축 (용량 절감)
                    .outputFormat(format)
                    .toOutputStream(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            log.error("[ImageProcessor] 이미지 리사이징 실패 - 원본 바이트 유지", e);
            throw new BusinessException(
                    GlobalErrorCode.INVALID_INPUT,
                    "이미지 리사이징에 실패했습니다."
            );
        }
    }

    /**
     * ImageIO 리더를 사용해 실제 이미지 포맷을 감지하고, 디코딩 전에 해상도(픽셀 수) 상한을 검사합니다.
     * 전체 픽셀 디코딩 없이 헤더 수준에서 width/height를 읽어 압축 폭탄(decompression bomb)을 방어합니다.
     */
    private ImageDimension detectAndValidateFormat(byte[] fileBytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(fileBytes))) {
            if (iis == null) {
                throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "손상되었거나 변조된 이미지 파일입니다.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "손상되었거나 변조된 이미지 파일입니다.");
            }

            // 파일 바이트를 해독할 수 있는 전용 디코더 객체
            ImageReader reader = readers.next();
            try {
                // seekForwardOnly: 스트림을 순방향으로만 읽어 메모리 절약
                // ignoreMetadata: 부가 메타데이터를 무시하여 읽기 속도를 최대로 끌어올림
                reader.setInput(iis, true, true);

                // 전체 이미지 픽셀을 메모리에 올리지 않고 가로, 세로 픽셀 크기만 즉시 읽어옴
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                // 초과 시 예외를 발생시켜 압축 폭탄 공격으로 인한 서버 메모리 고갈을 사전에 차단
                if ((long) width * height > MAX_PIXEL_COUNT) {
                    throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "이미지 해상도가 허용 범위를 초과했습니다.");
                }

                return new ImageDimension(reader.getFormatName().toLowerCase(), width, height);
            } finally {
                // 사용이 끝난 reader 객체를 메모리에서 해제
                reader.dispose();
            }
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
        // 읽어들인 조각 바이트들을 하나로 모아 저장할 메모리 스트림을 생성
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytes = 0;

            // 스트림의 끝(EOF)에 도달할 때까지 4KB 조각 단위로 계속 읽음
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                if (totalBytes + bytesRead > limit) {
                    throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "업로드 가능한 최대 파일 크기를 초과했습니다.");
                }
                totalBytes += bytesRead;
                bos.write(buffer, 0, bytesRead);
            }
            return bos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(GlobalErrorCode.INVALID_INPUT, "파일을 읽는 중 오류가 발생했습니다.");
        }
    }

    /**
     * 검증 및 리사이징이 완료된 바이트 데이터, 감지된 Content-Type, 검증된 확장자(".jpg" 등)를 담는 결과 객체.
     */
    public record ImageValidationResult(
            byte[] data,
            String detectedContentType,
            String extension,
            String format,
            int width,
            int height
    ) {
        public InputStream inputStream() {
            return new ByteArrayInputStream(data);
        }
    }

    /**
     * detectAndValidateFormat()의 반환값으로, 감지된 실제 이미지 포맷명과 픽셀 단위의 가로/세로 크기를 담습니다.
     * 리사이징 여부(원본이 목표 규격보다 작은지) 판단에 사용됩니다.
     */
    private record ImageDimension(String format, int width, int height) {
    }
}
