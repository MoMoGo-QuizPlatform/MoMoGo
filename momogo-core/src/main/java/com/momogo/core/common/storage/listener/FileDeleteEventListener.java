package com.momogo.core.common.storage.listener;

import com.momogo.core.common.config.AsyncConfig;
import com.momogo.core.common.storage.StorageService;
import com.momogo.core.common.storage.event.FileDeleteEvent;
import com.momogo.core.common.storage.event.FileRollbackDeleteEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * DB 트랜잭션 수명주기(AFTER_COMMIT, AFTER_ROLLBACK) 시점에 이벤트를 수신하여
 * 백그라운드 스레드(Virtual Thread)에서 파일 삭제를 비동기로 실행하는 리스너입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileDeleteEventListener {

    private final StorageService storageService;

    /**
     * DB 트랜잭션이 정상 커밋되었을 때 기존 파일 삭제를 비동기로 처리합니다.
     */
    @Async(AsyncConfig.FILE_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileDeleteEvent(FileDeleteEvent event) {
        if (event != null) {
            processFileDelete(event.fileUrl(), "커밋 완료 후 비동기로 기존 파일 삭제");
        }
    }

    /**
     * DB 트랜잭션이 롤백되었을 때 업로드했던 신규 파일 삭제를 비동기로 처리합니다.
     */
    @Async(AsyncConfig.FILE_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleFileRollbackDeleteEvent(FileRollbackDeleteEvent event) {
        if (event != null) {
            processFileDelete(event.fileUrl(), "커밋 롤백 후 비동기로 신규 파일 삭제");
        }
    }

    private void processFileDelete(String fileUrl, String contextMessage) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            storageService.delete(fileUrl);
            log.info("[FileDeleteEventListener] {} 완료: {}", contextMessage, fileUrl);
        } catch (Exception e) {
            log.error("[FileDeleteEventListener] {} 실패 - file: {}", contextMessage, fileUrl, e);
        }
    }
}
