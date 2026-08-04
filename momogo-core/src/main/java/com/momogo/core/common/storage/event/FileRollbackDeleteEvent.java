package com.momogo.core.common.storage.event;

/**
 * DB 트랜잭션 롤백(AFTER_ROLLBACK) 시 업로드했던 신규 임시 파일을 비동기로 삭제하는 이벤트입니다.
 *
 * @param fileUrl 삭제할 S3 Key 또는 파일 URL 경로
 */
public record FileRollbackDeleteEvent(String fileUrl) {
}
