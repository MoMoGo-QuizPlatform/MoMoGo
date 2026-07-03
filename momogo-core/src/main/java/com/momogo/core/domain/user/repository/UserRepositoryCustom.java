package com.momogo.core.domain.user.repository;

import com.momogo.core.domain.user.entity.enums.UserRole;
import java.util.UUID;

public interface UserRepositoryCustom {

  // 공간 탈퇴 벌크 업데이트용
  long bulkLeaveSpace(UUID spaceId, UserRole defaultRole);
}
