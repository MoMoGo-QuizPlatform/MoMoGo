package com.momogo.core.domain.user.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.security.PasswordEncryptor;
import com.momogo.core.common.storage.StorageService;
import com.momogo.core.domain.user.dto.request.UserCreateRequest;
import com.momogo.core.domain.user.dto.request.UserUpdateRequest;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.entity.enums.SocialType;
import com.momogo.core.domain.user.entity.enums.UserRole;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.mapper.UserMapper;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;

import org.springframework.dao.DataIntegrityViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncryptor passwordEncryptor;
    private final UserMapper userMapper;
    private final UserSessionService userSessionService;
    private final StorageService storageService;

    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncryptor.encrypt(request.password());

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(encodedPassword)
                .role(UserRole.USER)
                .social(SocialType.NONE)
                .isBanned(false)
                .build();

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return userMapper.toResponse(savedUser);
        } catch (DataIntegrityViolationException e) {
            log.error("[UserService] 이메일 중복 제약 조건 위반 발생 {}", request.email(), e);
            throw new BusinessException(UserErrorCode.ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request, ProfileImageUploadRequest profile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.NOT_FOUND));

        // 이름 변경
        if (request != null && request.name() != null && !request.name().isBlank()) {
            user.updateName(request.name());
        }

        // 프로필 이미지 변경
        if (profile != null) {
            String oldProfileImageUrl = user.getProfileImageUrl();

            // Try-with-resources로 스트림 자원을 안전하게 관리 (메모리 및 자원 누수 방지)
            try (InputStream inputStream = profile.inputStream()) {
                String savedFileName = storageService.upload(
                        inputStream,
                        profile.originalFilename(),
                        profile.contentType(),
                        "profile"
                );

                user.updateProfileImage(savedFileName);

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                if (status == STATUS_COMMITTED) {
                                    if (oldProfileImageUrl != null && !oldProfileImageUrl.isBlank()) {
                                        storageService.delete("profile/" + oldProfileImageUrl);
                                    }
                                } else if (status == STATUS_ROLLED_BACK) {
                                    storageService.delete("profile/" + savedFileName);
                                }
                            }
                        }
                );
            } catch (IOException e) {
                log.error("[UserService] 프로필 이미지 저장 중 예외 발생", e);
                throw new BusinessException(
                        GlobalErrorCode.FILE_UPLOAD_FAILED,
                        "파일 저장 중 시스템 오류가 발생했습니다."
                );
            }
        }

        if (request != null && request.newPassword() != null && !request.newPassword().isBlank()) {
            // 새 비밀번호는 입력했는데 현재 비밀번호가 누락된 경우
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new BusinessException(UserErrorCode.CURRENT_PASSWORD_REQUIRED);
            }

            // 현재 비밀번호가 일치하는지 검증
            if (!passwordEncryptor.matches(request.currentPassword(), user.getPassword())) {
                throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
            }

            String encodedPassword = passwordEncryptor.encrypt(request.newPassword());
            user.updatePassword(encodedPassword);

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            userSessionService.invalidateUserSessions(userId);
                        }
                    }
            );
        }

        return userMapper.toResponse(user);
    }
}
