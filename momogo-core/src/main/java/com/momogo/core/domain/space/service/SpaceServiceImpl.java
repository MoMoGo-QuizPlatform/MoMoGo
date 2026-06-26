package com.momogo.core.domain.space.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.exception.SpaceErrorCode;
import com.momogo.core.domain.space.mapper.SpaceMapper;
import com.momogo.core.domain.space.repository.SpaceRepository;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpaceServiceImpl implements SpaceService {

  private final SpaceRepository spaceRepository;
  private final UserRepository userRepository;
  private final SpaceMapper spaceMapper;

  @Override
  @Transactional
  public Space createSpace(UUID userId, SpaceCreateRequest request) {

    // 유저 검증, 공간 가입 상태 확인
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.USER_NOT_FOUND));

    if (user.getSpace() != null) {
      throw new BusinessException(SpaceErrorCode.ALREADY_IN_SPACE);
    }

    // 공간 개설 및 저장
    Space space = spaceMapper.toEntity(request, UUID.randomUUID());
    Space savedSpace = spaceRepository.save(space);

    // 개설한 유저의 공간 정보 및 역할을 ADMIN으로 업데이트
    // user.joinSpace(savedSpace, UserRole.ADMIN);

    return savedSpace;
  }

  @Override
  @Transactional
  public void joinSpace(UUID userId, UUID spaceId, String password) {

    // 유저 및 공간 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.USER_NOT_FOUND));

    if (user.getSpace() != null) {
      throw new BusinessException(SpaceErrorCode.ALREADY_IN_SPACE);
    }

    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    // 비밀번호 체크
    if (!space.getSpacePassword().equals(password)) {
      throw new BusinessException(SpaceErrorCode.WRONG_SPACE_PASSWORD);
    }

    // 공간 가입 처리
    //user.joinSpace(space, UserRole.USER);
  }

  @Override
  public Space getMySpace(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.USER_NOT_FOUND));

    return user.getSpace();
  }

  @Override
  public List<Space> getUnjoinedSpaces(UUID userId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.USER_NOT_FOUND));

    // 가입된 공간이 없으면 전체 조회, 있으면 본인 공간을 제외한 전체 조회
    if (user.getSpace() == null) {
      return spaceRepository.findAll();
    } else {
      return spaceRepository.findAllByIdNot(user.getSpace().getId());
    }
  }

  @Override
  @Transactional
  public Space updateSpace(UUID userId, UUID spaceId, SpaceUpdateRequest request) {

    // 유저 및 ADMIN 권한 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.USER_NOT_FOUND));

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }

    // 공간 조회 및 수정
    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    spaceMapper.updateFromDto(request, space); // MapStruct 더티 체킹

    return space;
  }

  @Override
  @Transactional
  public void deleteSpace(UUID userId, UUID spaceId) {

    // 유저 및 ADMIN 권한 검증
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.USER_NOT_FOUND));

    if (user.getRole() != UserRole.ADMIN || user.getSpace() == null || !user.getSpace().getId().equals(spaceId)) {
      throw new BusinessException(SpaceErrorCode.NOT_SPACE_ADMIN);
    }

    Space space = spaceRepository.findById(spaceId)
        .orElseThrow(() -> new BusinessException(SpaceErrorCode.SPACE_NOT_FOUND));

    // 소속되어 있는 모든 유저 탈퇴 처리
//    List<User> members = userRepository.findAllBySpaceId(spaceId);
//    for (User member : members) {
//      member.leaveSpace();
//    }

    spaceRepository.delete(space);
  }
}
