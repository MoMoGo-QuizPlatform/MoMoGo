package com.momogo.core.common.lock;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.domain.room.exception.RoomErrorCode;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * Redisson 분산 락 획득 및 해제를 담당하는 헬퍼 컴포넌트.
 * 분산 락 처리 중 발생하는 락 타임아웃, 인터럽트 및 언락 처리를 공통화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

  private final RedissonClient redissonClient;

  public void executeWithLock(String lockKey, long waitSeconds, Runnable action) {
    executeWithLock(lockKey, waitSeconds, () -> {
      action.run();
      return null;
    });
  }

  public <T> T executeWithLock(String lockKey, long waitSeconds, Supplier<T> supplier) {
    RLock lock = redissonClient.getLock(lockKey);
    try {
      if (!lock.tryLock(waitSeconds, TimeUnit.SECONDS)) {
        log.warn("[DistributedLockExecutor] 분산 락 획득 실패 - lockKey: {}", lockKey);
        throw new BusinessException(RoomErrorCode.LOCK_ACQUISITION_FAILED);
      }
      try {
        return supplier.get();
      } finally {
        if (lock.isHeldByCurrentThread()) {
          lock.unlock();
        }
      }
    } catch (InterruptedException e) {
      log.error("[DistributedLockExecutor] 분산 락 대기 중 인터럽트 발생 - lockKey: {}", lockKey, e);
      Thread.currentThread().interrupt();
      throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR, "락 대기 중 인터럽트가 발생했습니다.");
    }
  }
}
