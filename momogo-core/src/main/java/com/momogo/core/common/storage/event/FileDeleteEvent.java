package com.momogo.core.common.storage.event;

/**
 * DB 트랜잭션 성공 완료(AFTER_COMMIT) 후 비동기 파일 삭제 이벤트입니다.
 *
 * @param fileUrl 삭제할 S3 Key 또는 파일 URL 경로
 */
public record FileDeleteEvent(String fileUrl) {
}
