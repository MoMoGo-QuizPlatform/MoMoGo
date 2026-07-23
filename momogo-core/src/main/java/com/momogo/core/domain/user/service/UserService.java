package com.momogo.core.domain.user.service;

import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;
import com.momogo.core.domain.user.dto.request.UserCreateRequest;
import com.momogo.core.domain.user.dto.request.UserPageRequest;
import com.momogo.core.domain.user.dto.request.UserUpdateRequest;
import com.momogo.core.domain.user.dto.response.CursorResponse;
import com.momogo.core.domain.user.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse updateUser(UUID userId, UserUpdateRequest request, ProfileImageUploadRequest profile);

    void softDeleteUser(UUID userId);

    void deleteExpiredUsers();

    void restoreUser(String email, String password);

    CursorResponse<UserResponse> findAllUsers(UserPageRequest request);

    /**
     * 요청 유저가 소속된 공간에 함께 속한 전체 유저 목록을 조회합니다. (응시 대상자 지정 등에 사용)
     *
     * @param userId 요청 유저 식별자
     * @return 같은 공간에 소속된 유저 목록 DTO (소속 공간이 없으면 빈 리스트)
     */
    List<UserResponse> findUsersInMySpace(UUID userId);

    /**
     * 유저의 정지(밴) 상태를 업데이트합니다.
     *
     * @param userId 대상 유저 식별자
     * @param banned 정지 여부
     * @return 변경된 유저 정보 DTO
     */
    UserResponse updateUserBannedStatus(UUID userId, boolean banned);
}
