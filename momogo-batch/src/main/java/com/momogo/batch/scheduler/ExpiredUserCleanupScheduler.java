package com.momogo.batch.scheduler;

import com.momogo.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredUserCleanupScheduler {

    private final UserService userService;

    /**
     * 회원 탈퇴 후 유효 기간(30일)이 만료된 사용자를 시스템에서 영구(물리) 삭제합니다.
     *
     * 매일 새벽 4시에 백그라운드 태스크로 작동하도록 스케줄이 등록되어 있습니다.
     * 크론식 구성: 초(0) 분(0) 시(4) 일(*) 월(*) 요일(*)
     */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "expiredUserCleanupJob", lockAtMostFor = "PT30M")
    public void runExpiredUserCleanupJob() {
        log.info("[ExpiredUserCleanupScheduler] 30일 경과 회원 물리 삭제 스케줄 작업 시작");
        try {
            userService.deleteExpiredUsers();
            log.info("[ExpiredUserCleanupScheduler] 30일 경과 회원 물리 삭제 스케줄 작업 성공적으로 종료");
        } catch (Exception e) {
            log.error("[ExpiredUserCleanupScheduler] 만료 회원 물리 삭제 작업 수행 중 예외 발생", e);
        }
    }
}

