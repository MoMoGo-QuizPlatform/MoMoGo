package com.momogo.core.domain.user.repository;

import com.momogo.core.domain.user.dto.UserSearchCondition;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserRepositoryCustom {

  List<User> findAllByCursor(UserSearchCondition condition, Pageable pageable);

  long countByCondition(UserSearchCondition condition);

  // 공간 탈퇴 벌크 업데이트용
  long bulkLeaveSpace(UUID spaceId, UserRole defaultRole);
}
