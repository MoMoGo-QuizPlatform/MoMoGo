package com.momogo.api.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momogo.api.auth.dto.JwtInformation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT 토큰 세션을 Redis 상에서 중앙 관리하는 레지스트리 구현체입니다.
 * Redisson 분산락을 사용하여 다중 서버 환경에서도 동시 로그인 제한 및 RTR(Token Rotation)의 동시성을 안전하게 제어합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jwt.registry", havingValue = "redis", matchIfMissing = true)
public class RedisJwtRegistry implements JwtRegistry {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Value("${app.jwt.max-active-count}")
    private int maxActiveCount;

    @Value("${jwt.access-token.exp}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token.exp}")
    private long refreshTokenExpirationMs;

    private static final String KEY_USER_PREFIX = "jwt:user:";
    private static final String KEY_ACCESS_PREFIX = "jwt:access:";
    private static final String KEY_REFRESH_PREFIX = "jwt:refresh:";
    private static final String LOCK_PREFIX = "lock:jwt:";

    /**
     * 신규 발급된 JWT 토큰 세션 정보를 Redis에 등록합니다.
     * 사용자별 활성 토큰 개수가 maxActiveCount를 초과할 경우 가장 오래된 세션을 자동으로 무효화(FIFO)합니다.
     *
     * @param jwtInformation 등록할 신규 JWT 세션 정보
     */
    @Override
    public void registerJwtInformation(JwtInformation jwtInformation) {
        if (jwtInformation == null) return;
        if (maxActiveCount <= 0) {
            log.warn("[RedisJwtRegistry] app.jwt.max-active-count 설정값이 0 이하입니다({}). 세션 등록 및 관리를 수행하지 않습니다.", maxActiveCount);
            return;
        }
        UUID userId = jwtInformation.user().id();

        executeWithLock(userId, () -> doRegisterJwtInformation(jwtInformation));
    }

    /**
     * 특정 사용자의 모든 활성 JWT 세션을 강제로 무효화(로그아웃/정지/탈퇴) 처리합니다.
     *
     * @param userId 무효화할 사용자의 식별자
     */
    @Override
    public void invalidateJwtInformationByUserId(UUID userId) {
        if (userId == null) return;

        executeWithLock(userId, () -> {
            String userKey = KEY_USER_PREFIX + userId;
            List<String> sessions = redisTemplate.opsForList().range(userKey, 0, -1);
            if (sessions != null) {
                for (String sessionJson : sessions) {
                    removeIndices(parseJwtInfo(sessionJson));
                }
            }
            redisTemplate.delete(userKey);
        });
    }

    /**
     * Access Token의 현재 활성화(유효) 여부를 확인합니다.
     *
     * @param accessToken 검증할 Access Token
     * @return 활성 세션에 존재하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) return false;
        return redisTemplate.hasKey(KEY_ACCESS_PREFIX + accessToken);
    }

    /**
     * Refresh Token의 현재 활성화(유효) 여부를 확인합니다.
     *
     * @param refreshToken 검증할 Refresh Token
     * @return 활성 세션에 존재하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return false;
        return redisTemplate.hasKey(KEY_REFRESH_PREFIX + refreshToken);
    }

    /**
     * Refresh Token을 활용하여 해당하는 활성 JWT 세션 정보를 조회합니다.
     *
     * @param refreshToken 조회할 Refresh Token
     * @return 유효한 JwtInformation 객체, 없거나 무효화된 경우 null
     */
    @Override
    public JwtInformation getJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return null;
        String json = redisTemplate.opsForValue().get(KEY_REFRESH_PREFIX + refreshToken);
        return parseJwtInfo(json);
    }

    /**
     * 토큰 재발급(RTR - Refresh Token Rotation) 수행 시 기존 세션을 폐기하고 신규 토큰 세션으로 교체 등록합니다.
     *
     * @param refreshToken      폐기할 이전 Refresh Token
     * @param newJwtInformation 새롭게 등록할 신규 토큰 세션 정보
     */
    @Override
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        if (newJwtInformation == null) return;
        UUID userId = newJwtInformation.user().id();

        executeWithLock(userId, () -> {
            // 이전 Refresh Token으로 유효한 정보 삭제
            if (refreshToken != null) {
                String oldJson = redisTemplate.opsForValue().get(KEY_REFRESH_PREFIX + refreshToken);
                if (oldJson != null) {
                    JwtInformation oldInfo = parseJwtInfo(oldJson);
                    removeIndices(oldInfo);
                    redisTemplate.opsForList().remove(KEY_USER_PREFIX + userId, 0, oldJson);
                }
            }
            // 락 중복 획득 없이 내부 등록 로직 직접 호출
            doRegisterJwtInformation(newJwtInformation);
        });
    }

    /**
     * 토큰 재발급(RTR) 실패 시 수행되는 롤백 로직입니다.
     * 신규 발급하려던 토큰 정보를 무효화하고 기존 토큰 정보로 원복합니다.
     *
     * @param oldRefreshToken   원복할 이전 Refresh Token
     * @param oldJwtInformation 원복할 이전 토큰 세션 정보
     * @param newRefreshToken   무효화 처리할 신규 Refresh Token
     */
    @Override
    public void rollbackRotateJwtInformation(String oldRefreshToken, JwtInformation oldJwtInformation, String newRefreshToken) {
        if (oldJwtInformation == null) return;
        UUID userId = oldJwtInformation.user().id();

        executeWithLock(userId, () -> {
            // 새로 발급되었던 신규 토큰 제거
            if (newRefreshToken != null) {
                String newJson = redisTemplate.opsForValue().get(KEY_REFRESH_PREFIX + newRefreshToken);
                if (newJson != null) {
                    removeIndices(parseJwtInfo(newJson));
                    redisTemplate.opsForList().remove(KEY_USER_PREFIX + userId, 0, newJson);
                }
            }
            // 이전 토큰 원복
            doRegisterJwtInformation(oldJwtInformation);
        });
    }

    /**
     * 락이 이미 확보된 상태에서 실행되는 실제 세션 등록 헬퍼 메소드입니다. (Self-Locking 방지)
     *
     * @param jwtInformation 등록할 토큰 세션 정보
     */
    private void doRegisterJwtInformation(JwtInformation jwtInformation) {
        if (maxActiveCount <= 0) {
            log.warn("[RedisJwtRegistry] app.jwt.max-active-count 설정값이 0 이하입니다({}). 세션 등록 및 관리를 수행하지 않습니다.", maxActiveCount);
            return;
        }

        UUID userId = jwtInformation.user().id();
        String userKey = KEY_USER_PREFIX + userId;

        // 허용 세션 수 초과 시 오래된 세션 제거 (FIFO)
        Long size = redisTemplate.opsForList().size(userKey);
        while (size != null && size >= maxActiveCount) {
            String oldSessionJson = redisTemplate.opsForList().leftPop(userKey);
            if (oldSessionJson == null) {
                break;
            }
            removeIndices(parseJwtInfo(oldSessionJson));
            size = redisTemplate.opsForList().size(userKey);
        }

        String json = toJson(jwtInformation);
        redisTemplate.opsForList().rightPush(userKey, json);
        // 유저 세션 리스트 키에도 TTL 설정 (메모리 누수 방지)
        redisTemplate.expire(userKey, Duration.ofMillis(refreshTokenExpirationMs));

        putIndices(jwtInformation);
    }

    /**
     * Redisson Watchdog을 활용하여 사용자 단위(userId)의 안전한 분산락 획득 및 해제를 수행하는 템플릿 메소드입니다.
     * <p>
     * <b>Redisson Watchdog 메커니즘:</b><br>
     * leaseTime을 지정하지 않으면 Redisson Watchdog이 자동으로 활성화되어,
     * action.run() 작업이 진행 중인 동안 10초마다 Redis 락의 만료 시간(TTL)을 30초로 계속 자동 연장합니다.
     * 작업이 정상 종료(unlock)되거나 서버가 다운되면 락이 자동 해제되어 데드락을 방지합니다.
     * </p>

     * @param userId 락 기준이 될 사용자 식별자
     * @param action 락 획득 후 실행할 임계 영역(Critical Section) 로직
     */
    private void executeWithLock(UUID userId, Runnable action) {
        // 1. 사용자 UUID 기반의 고유한 분산락 키 생성 (lock:jwt:{userId})
        RLock lock = redissonClient.getLock(LOCK_PREFIX + userId);
        try {
            // 2. 락 획득 시도 (대기시간: 3초)
            // leaseTime을 넘기지 않아야 Redisson Watchdog(자동 락 만료 연장)이 작동합니다.
            if (lock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    // 3. 실제 임계 영역(Critical Section) 비즈니스 로직 실행
                    action.run();
                } finally {
                    // 4. 작업이 끝나면 현재 스레드가 잡고 있는 락인지 확인 후 안전하게 해제
                    // (lock.unlock() 호출 시 Watchdog도 함께 자동 종료됨)
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // 3초 대기 후에도 락 획득에 실패한 경우
                log.warn("[RedisJwtRegistry] 락 획득 타임아웃 발생 - userId: {}", userId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[RedisJwtRegistry] 락 획득 중 인터럽트 발생 - userId: {}", userId, e);
        }
    }

    /**
     * Access Token 및 Refresh Token 조회를 위한 Redis 색인(Index) Key-Value 데이터를 저장합니다.
     *
     * @param info 등록할 토큰 세션 정보
     */
    private void putIndices(JwtInformation info) {
        String json = toJson(info);
        if (info.accessToken() != null) {
            redisTemplate.opsForValue().set(
                    KEY_ACCESS_PREFIX + info.accessToken(),
                    json,
                    Duration.ofMillis(accessTokenExpirationMs)
            );
        }
        if (info.refreshToken() != null) {
            redisTemplate.opsForValue().set(
                    KEY_REFRESH_PREFIX + info.refreshToken(),
                    json,
                    Duration.ofMillis(refreshTokenExpirationMs)
            );
        }
    }

    /**
     * Access Token 및 Refresh Token 조회를 위한 Redis 색인(Index) Key를 삭제합니다.
     *
     * @param info 제거할 토큰 세션 정보
     */
    private void removeIndices(JwtInformation info) {
        if (info == null) return;
        if (info.accessToken() != null) {
            redisTemplate.delete(KEY_ACCESS_PREFIX + info.accessToken());
        }
        if (info.refreshToken() != null) {
            redisTemplate.delete(KEY_REFRESH_PREFIX + info.refreshToken());
        }
    }

    /**
     * JwtInformation 객체를 JSON 문자열로 직렬화합니다.
     *
     * @param info 직렬화할 JwtInformation 객체
     * @return 변환된 JSON 문자열
     */
    private String toJson(JwtInformation info) {
        try {
            return objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            throw new RuntimeException("JWT Info 직렬화 실패", e);
        }
    }

    /**
     * JSON 문자열을 JwtInformation 객체로 역직렬화합니다.
     *
     * @param json 역직렬화할 JSON 문자열
     * @return 변환된 JwtInformation 객체 (실패 시 null)
     */
    private JwtInformation parseJwtInfo(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, JwtInformation.class);
        } catch (Exception e) {
            return null;
        }
    }
}

