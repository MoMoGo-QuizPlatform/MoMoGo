package com.momogo.core.common.config;

/**
 * Room 도메인 관련 Redis 키 상수.
 * 서비스와 컨슈머 간 동일한 Redis 키를 안전하게 공유하기 위해 관리한다.
 */
public final class RoomRedisKeys {

    public static final String SUBMIT_LOCK_PREFIX = "lock:room:submit:";
    public static final String SUBMIT_CLAIM_PREFIX = "room:submit:claimed:";
    public static final String DEDUP_KEY_PREFIX = "room:submit:processed:";

    private RoomRedisKeys() {
    }
}
