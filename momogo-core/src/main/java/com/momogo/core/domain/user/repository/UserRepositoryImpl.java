package com.momogo.core.domain.user.repository;

import com.momogo.core.domain.user.entity.QUser;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final EntityManager em; // 1차 캐시 비우기용

  @Override
  public long bulkLeaveSpace(UUID spaceId, UserRole defaultRole) {
    QUser user = QUser.user;

    // 벌크 update 실행
    long affectedRows = queryFactory.update(user)
        .setNull(user.space)
        .set(user.role, defaultRole)
        .where(user.space.id.eq(spaceId))
        .execute();

    // 1차 캐시 클리어(벌크 연산이 1차 캐시를 거치지 않기 때문)
    em.flush();
    em.clear();

    return affectedRows;
  }
}
