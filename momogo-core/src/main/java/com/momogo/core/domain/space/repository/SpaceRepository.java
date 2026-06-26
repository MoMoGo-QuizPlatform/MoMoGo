package com.momogo.core.domain.space.repository;

import com.momogo.core.domain.space.entity.Space;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SpaceRepository extends JpaRepository<Space, UUID>, SpaceRepositoryCustom {

  // 특정 공간 ID를 제외한 전체 공간 목록 조회 (미가입 공간 탐색용)
  List<Space> findAllByIdNot(UUID spaceId);
}
