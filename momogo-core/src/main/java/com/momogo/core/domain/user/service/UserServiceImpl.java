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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;

import org.springframework.dao.DataIntegrityViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
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

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    /**
     * 신규 회원 가입을 처리합니다.
     * 이메일 중복 검사 및 예약된 이메일 가입 방지 검증을 포함합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 가입 완료된 유저 정보 DTO
     * @throws BusinessException 이메일이 중복되었거나 사용할 수 없는 이메일인 경우
     */
    // TODO: 회원 가입 시 해당 실제 이메일이 존재하는지 검증 로직 구현
    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 일반 유저가 이미 선점된 Email 주소로 회원가입하는 것을 방지
        if (request.email().equalsIgnoreCase(superAdminEmail)) {
            throw new BusinessException(UserErrorCode.RESERVED_EMAIL);
        }

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
            if (isDuplicateEmailViolation(e)) {
                log.warn("[UserService] 이메일 중복 제약 조건 위반 발생");
                throw new BusinessException(UserErrorCode.ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /**
     * 회원 프로필(이름, 프로필 이미지) 및 비밀번호를 수정합니다.
     * 임시 비밀번호가 활성화된 경우 이를 확인 및 무효화(초기화) 처리합니다.
     *
     * @param userId 회원 식별자
     * @param request 이름 및 패스워드 변경 정보 DTO
     * @param profile 업로드할 프로필 이미지 정보 DTO
     * @return 수정 완료된 유저 정보 DTO
     * @throws BusinessException 유저를 찾을 수 없거나, 소셜 계정의 비밀번호 수정을 시도하거나, 현재 비밀번호가 불일치하는 경우
     */
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

            // 소셜 로그인 가입자 비밀번호 변경 시도 차단
            if (user.getSocial() != SocialType.NONE) {
                throw new BusinessException(UserErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
            }

            // 새 비밀번호는 입력했는데 현재 비밀번호가 누락된 경우
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new BusinessException(UserErrorCode.CURRENT_PASSWORD_REQUIRED);
            }

            // 비밀 번호 검증 (영구 비밀번호 혹은 유효한 임시 비밀번호 중 하나라도 부합하는지 체크)
            boolean isCurrentPasswordValid = passwordEncryptor.matches(request.currentPassword(), user.getPassword())
                    || isValidTemporaryPassword(user, request.currentPassword());

            if (!isCurrentPasswordValid) {
                throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
            }

            String encodedPassword = passwordEncryptor.encrypt(request.newPassword());
            user.updatePassword(encodedPassword);

            user.clearTemporaryPassword();

            userSessionService.invalidateUserSessions(userId);
        }

        return userMapper.toResponse(user);
    }

    /**
     * DB 제약 조건 예외(DataIntegrityViolationException)가 이메일 유니크 제약(UQ_USER_EMAIL_ACTIVE) 위반인지 판단합니다.
     */
    private boolean isDuplicateEmailViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        String message = cause.getMessage();
        if (message == null) {
            return false;
        }
        return message.toLowerCase().contains("uq_user_email_active");
    }

    /**
     * 임시 비밀번호가 존재하고, 10분 유효시간 이내이며, 입력받은 평문 비밀번호와 일치하는지 판단합니다.
     */
    private boolean isValidTemporaryPassword(User user, String rawPassword) {
        // 임시 비밀번호가 비어 있는지 확인
        if (user.getTempPassword() == null || user.getTempPasswordExpiredAt() == null) {
            return false;
        }
        if (OffsetDateTime.now().isAfter(user.getTempPasswordExpiredAt())) {
            return false;
        }
        return passwordEncryptor.matches(rawPassword, user.getTempPassword());
    }
}
