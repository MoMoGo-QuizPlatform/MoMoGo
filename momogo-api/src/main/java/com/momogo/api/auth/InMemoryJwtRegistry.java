package com.momogo.api.auth;

import com.momogo.api.auth.dto.JwtInformation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 토큰 세션을 메모리(In-Memory) 상에서 관리하는 레지스트리 구현체입니다.
 * 다중 로그인 제어(기기 대수 제한)를 위해 사용자당 Queue 구조를 사용하며,
 * O(1) 수준의 조회를 보장하기 위해 토큰 인덱스 맵을 별도로 관리합니다.
 * 사용자별 Lock을 설정하여 멀티스레드 환경에서도 원자성과 일관성을 보장합니다.
 */
@Service
@ConditionalOnProperty(name = "app.jwt.registry", havingValue = "memory")
public class InMemoryJwtRegistry implements JwtRegistry {

    // 사용자 식별자(UUID)별 활성 토큰 세션 큐 (기기 제한 관리용)
    private final Map<UUID, Queue<JwtInformation>> origin;
    
    // 사용자당 허용할 최대 활성 토큰 세션 수 (중복 로그인 제한 개수)
    private final int maxActiveJwtCount;
    
    // Access Token으로 세션 정보를 즉시 조회하기 위한 인덱스 맵
    private final Map<String, JwtInformation> tokenIndex;
    
    // Refresh Token으로 세션 정보를 즉시 조회하기 위한 인덱스 맵 (조회 최적화용)
    private final Map<String, JwtInformation> refreshTokenIndex;

    // 사용자별 동시성 제어를 위한 고정 크기 락 객체 배열 (Striped Lock)
    private static final int LOCK_POOL_SIZE = 128;
    private final Object[] locks;

    public InMemoryJwtRegistry() {
        this.origin = new ConcurrentHashMap<>();
        this.maxActiveJwtCount = 1;
        this.tokenIndex = new ConcurrentHashMap<>();
        this.refreshTokenIndex = new ConcurrentHashMap<>();
        
        this.locks = new Object[LOCK_POOL_SIZE];
        for (int i = 0; i < LOCK_POOL_SIZE; i++) {
            locks[i] = new Object();
        }
    }

    /**
     * 사용자 식별자별 고유 Lock 객체를 획득합니다.
     * 해시 버킷 인덱스를 활용하여 고정된 락 풀에서 객체를 반환합니다.
     */
    private Object lockFor(UUID userId) {
        int index = Math.abs(userId.hashCode() % LOCK_POOL_SIZE);
        return locks[index];
    }

    /**
     * 신규 발급된 JWT 토큰 세션 정보를 등록합니다.
     * 사용자별 활성 토큰 큐의 크기가 maxActiveJwtCount를 초과할 경우,
     * 가장 오래전에 로그인된 토큰 세션을 자동으로 폐기(만료 처리)합니다.
     *
     * @param jwtInformation 신규 로그인/토큰 세션 정보
     */
    @Override
    @Transactional
    public void registerJwtInformation(JwtInformation jwtInformation) {
        if (jwtInformation == null) return;

        UUID userId = jwtInformation.user().id();

        // 짧은 시간 발생하는 동시성 경쟁 상태를 차단
        synchronized (lockFor(userId)) {
            Queue<JwtInformation> userQueue = origin.computeIfAbsent(userId, ignored -> new ArrayDeque<>());

            // 허용된 세션 개수를 초과하는 경우, FIFO 순서대로 기존 로그인 세션 제거
            while (userQueue.size() >= maxActiveJwtCount) {
                JwtInformation removed = userQueue.poll();
                if (removed != null) {
                    removeIndices(removed);
                }
            }

            // 새로운 토큰 세션을 큐에 추가하고 인덱스 맵에 등록
            userQueue.offer(jwtInformation);
            putIndices(jwtInformation);
        }
    }

    /**
     * 특정 사용자의 모든 활성 토큰 세션을 강제로 폐기(로그아웃)합니다.
     * 비밀번호 변경, 계정 잠금, 강제 로그아웃 등의 시나리오에서 호출됩니다.
     *
     * @param userId 무효화할 사용자의 식별자
     */
    @Override
    @Transactional
    public void invalidateJwtInformationByUserId(UUID userId) {
        if (userId == null) return;

        synchronized (lockFor(userId)) {
            // 사용자의 토큰 세션 큐를 제거하고, 해당 큐에 있던 모든 토큰 인덱스를 삭제
            Queue<JwtInformation> removedQueue = origin.remove(userId);
            if (removedQueue != null) {
                removedQueue.forEach(this::removeIndices);
            }
        }
    }

    /**
     * 전달받은 Access Token이 현재 활성화되어 있는 유효한 세션인지 확인합니다.
     *
     * @param accessToken 검증할 Access Token
     * @return 활성 세션에 존재하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
        if (accessToken == null) return false;
        return tokenIndex.containsKey(accessToken);
    }

    /**
     * 전달받은 Refresh Token이 현재 활성화되어 있는 유효한 세션인지 확인합니다.
     *
     * @param refreshToken 검증할 Refresh Token
     * @return 활성 세션에 존재하면 true, 그렇지 않으면 false
     */
    @Override
    public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return false;
        return refreshTokenIndex.containsKey(refreshToken);
    }

    /**
     * Refresh Token을 활용하여 해당하는 활성 JWT 세션 정보를 조회합니다.
     *
     * @param refreshToken 조회할 Refresh Token
     * @return 존재할 경우 JwtInformation 객체, 없거나 무효화된 경우 null
     */
    @Override
    public JwtInformation getJwtInformationByRefreshToken(String refreshToken) {
        if (refreshToken == null) return null;
        return refreshTokenIndex.get(refreshToken);
    }

    /**
     * 토큰 재발급(Rotation - RTR) 시 기존 Refresh Token 세션을 폐기하고,
     * 새롭게 갱신된 신규 토큰 세션 정보로 교체 등록합니다.
     *
     * @param refreshToken      폐기할 이전 Refresh Token
     * @param newJwtInformation 새롭게 등록할 신규 토큰 세션 정보
     */
    @Override
    @Transactional
    public void rotateJwtInformation(String refreshToken, JwtInformation newJwtInformation) {
        if (newJwtInformation == null) return;

        UUID userId = newJwtInformation.user().id();

        synchronized (lockFor(userId)) {
            Queue<JwtInformation> userQueue = origin.computeIfAbsent(userId, ignored -> new ArrayDeque<>());

            // 사용되었던 이전 리프레시 토큰 정보를 큐 및 인덱스 맵에서 완전 제거
            if (refreshToken != null) {
                userQueue.removeIf(info -> {
                    boolean matched = refreshToken.equals(info.refreshToken());
                    if (matched) {
                        removeIndices(info);
                    }
                    return matched;
                });
            }

            // 세션 개수 초과분 정리
            while (userQueue.size() >= maxActiveJwtCount) {
                JwtInformation polled = userQueue.poll();
                if (polled != null) {
                    removeIndices(polled);
                }
            }

            // 새로운 토큰 세션 정보 등록
            userQueue.offer(newJwtInformation);
            putIndices(newJwtInformation);
        }
    }

    /**
     * 토큰 재발급(Rotation) 중 네트워크 오류 등의 예외 발생 시 수행되는 롤백 로직입니다.
     * 새로 발급하려던 신규 토큰 정보를 무효화하고, 이전에 유효했던 기존 토큰 정보로 원복합니다.
     *
     * @param oldRefreshToken  원복할 이전 Refresh Token
     * @param oldJwtInformation 원복할 이전 토큰 세션 정보
     * @param newRefreshToken   무효화 처리할 신규 Refresh Token
     */
    @Override
    @Transactional
    public void rollbackRotateJwtInformation(
            String oldRefreshToken,
            JwtInformation oldJwtInformation,
            String newRefreshToken
    ) {
        if (oldJwtInformation == null) return;

        UUID userId = oldJwtInformation.user().id();

        synchronized (lockFor(userId)) {
            Queue<JwtInformation> userQueue = origin.computeIfAbsent(userId, ignored -> new ArrayDeque<>());

            // 롤백 처리를 위해 새로 발급된 신규 토큰 정보 제거
            if (newRefreshToken != null) {
                userQueue.removeIf(info -> {
                    boolean matched = newRefreshToken.equals(info.refreshToken());
                    if (matched) {
                        removeIndices(info);
                    }
                    return matched;
                });
            }

            // 이전 토큰 정보가 현재 큐에 없는 경우에만 복구 등록 진행
            boolean oldTokenAlreadyExists = userQueue.stream()
                    .anyMatch(info -> oldRefreshToken != null && oldRefreshToken.equals(info.refreshToken()));

            if (!oldTokenAlreadyExists) {
                while (userQueue.size() >= maxActiveJwtCount) {
                    JwtInformation removed = userQueue.poll();
                    if (removed != null) {
                        removeIndices(removed);
                    }
                }

                userQueue.offer(oldJwtInformation);
                putIndices(oldJwtInformation);
            }
        }
    }

    /**
     * 주입받은 토큰 정보의 Access Token 및 Refresh Token을 각각 인덱스 맵에 등록합니다.
     *
     * @param info 등록할 토큰 세션 정보
     */
    private void putIndices(JwtInformation info) {
        if (info.accessToken() != null) {
            tokenIndex.put(info.accessToken(), info);
        }
        if (info.refreshToken() != null) {
            refreshTokenIndex.put(info.refreshToken(), info);
        }
    }

    /**
     * 주입받은 토큰 정보의 Access Token 및 Refresh Token을 각각 인덱스 맵에서 삭제합니다.
     *
     * @param info 제거할 토큰 세션 정보
     */
    private void removeIndices(JwtInformation info) {
        if (info.accessToken() != null) {
            tokenIndex.remove(info.accessToken());
        }
        if (info.refreshToken() != null) {
            refreshTokenIndex.remove(info.refreshToken());
        }
    }
}
