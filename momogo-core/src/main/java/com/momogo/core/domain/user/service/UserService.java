package com.momogo.core.domain.user.service;

import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;
import com.momogo.core.domain.user.dto.request.UserCreateRequest;
import com.momogo.core.domain.user.dto.request.UserPageRequest;
import com.momogo.core.domain.user.dto.request.UserUpdateRequest;
import com.momogo.core.domain.user.dto.response.CursorResponse;
import com.momogo.core.domain.user.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse updateUser(UUID userId, UserUpdateRequest request, ProfileImageUploadRequest profile);

    void softDeleteUser(UUID userId);

    void deleteExpiredUsers();

    void restoreUser(String restoreToken, String password);

    CursorResponse<UserResponse> findAllUsers(UserPageRequest request);
}
