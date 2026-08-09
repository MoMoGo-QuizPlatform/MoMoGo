package com.momogo.core.domain.user.event;

/**
 * 유저의 DB 상태가 변경되어 Redis와 user_dtos 캐시를 무효화할 때 발생하는 이벤트
 * 트랜잭션이 커밋될 경우에만 Redis 캐시가 적용되어야 함
 *
 * @param email 정규화된 이메일 - 캐시 키와 동일
 */
public record UserCacheEvictEvent(String email) {
}
