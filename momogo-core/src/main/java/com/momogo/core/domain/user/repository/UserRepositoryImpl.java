package com.momogo.core.domain.user.repository;

import com.momogo.core.domain.user.dto.UserSearchCondition;
import com.momogo.core.domain.user.entity.QUser;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static com.momogo.core.domain.user.entity.QUser.user;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager em; // 1차 캐시 비우기용

    @Override
    public List<User> findAllByCursor(UserSearchCondition condition, Pageable pageable) {
        return queryFactory
                .selectFrom(user)
                .where(
                        nameLike(condition.nameLike()),
                        emailLike(condition.emailLike()),
                        // 정렬 기준에 따라 활성/탈퇴 회원을 동적으로 분기하여 필터링
                        filterBySortBy(condition.sortBy()),
                        cursorCondition(condition.sortBy(), condition.cursor(), condition.idAfter(), condition.sortDirection())
                )
                .orderBy(createOrderSpecifier(condition.sortBy(), condition.sortDirection()))
                .limit(pageable.getPageSize())
                .fetch();
    }

    @Override
    public long countByCondition(UserSearchCondition condition) {
        Long count = queryFactory
                .select(user.count())
                .from(user)
                .where(
                        nameLike(condition.nameLike()),
                        emailLike(condition.emailLike()),
                        filterBySortBy(condition.sortBy())
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression nameLike(String nameLike) {
        return nameLike != null && !nameLike.isBlank() ? user.name.contains(nameLike) : null;
    }

    private BooleanExpression emailLike(String emailLike) {
        return emailLike != null && !emailLike.isBlank() ? user.email.contains(emailLike) : null;
    }

    private DateTimePath<OffsetDateTime> getSortPath(String sortBy) {
        return switch (sortBy) {
            case "updatedAt" -> user.updatedAt;
            case "deletedAt" -> user.deletedAt;
            default -> user.createdAt;
        };
    }

    // 비정상적인 커서 문자열 인입 시 첫페이지를 보여주는 유연한 처리를 한다.
    private BooleanExpression cursorCondition(String sortBy, String cursor, UUID idAfter, SortDirection sortDirection) {
        if (cursor == null || cursor.isBlank() || idAfter == null) {
            return null;
        }

        final OffsetDateTime cursorTime;
        try {
            cursorTime = OffsetDateTime.parse(cursor);
        } catch (DateTimeParseException e) {
            log.warn("유효하지 않은 형식의 커서 전달됨: {}", cursor);
            return null;
        }

        DateTimePath<OffsetDateTime> sortPath = getSortPath(sortBy);

        if (sortDirection == SortDirection.ASCENDING) {
            return sortPath.gt(cursorTime)
                    .or(sortPath.eq(cursorTime).and(user.id.gt(idAfter)));
        }

        return sortPath.lt(cursorTime)
                .or(sortPath.eq(cursorTime).and(user.id.lt(idAfter)));
    }

    // 계정을  삭제하지 않은 유저는 deletedAt = null이므로
    // Super Admin이 deletedAt을 필터 조건으로 사용했을 경우 deletedAt != null인 삭제 요청한 유저만 확인할 수 있다.
    private BooleanExpression filterBySortBy(String sortBy) {
        if ("deletedAt".equals(sortBy)) {
            return user.deletedAt.isNotNull();
        }
        return user.deletedAt.isNull();
    }

    // 1차 정렬 조건 필드와 2차 순서 보장용 고유 ID(UUID) 정렬을 결합하여 복합 정렬 순서 정의
    private OrderSpecifier<?>[] createOrderSpecifier(String sortBy, SortDirection sortDirection) {
        DateTimePath<OffsetDateTime> sortPath = getSortPath(sortBy);

        if (sortDirection == SortDirection.ASCENDING) {
            return new OrderSpecifier[]{
                    sortPath.asc(),
                    user.id.asc()
            };
        }
        return new OrderSpecifier[]{
                sortPath.desc(),
                user.id.desc()
        };
    }

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
