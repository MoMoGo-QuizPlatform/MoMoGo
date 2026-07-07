package com.momogo.core.domain.user.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.exception.GlobalErrorCode;
import com.momogo.core.common.security.PasswordEncryptor;
import com.momogo.core.common.storage.StorageService;
import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final String PROFILE_IMAGE_DIR = "profile";

    private final UserRepository userRepository;
    private final PasswordEncryptor passwordEncryptor;
    private final UserMapper userMapper;
    private final UserSessionService userSessionService;
    private final StorageService storageService;

    private final UserHardDeleteProcessor hardDeleteProcessor;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    /**
     * 신규 회원 가입을 처리합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 가입 완료된 회원 정보 DTO
     */
    // TODO: 회원 가입 시 해당 실제 이메일이 존재하는지 검증 로직 구현
    @Override
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 일반 유저가 이미 선점된 Email 주소로 회원가입하는 것을 방지
        if (request.email().equalsIgnoreCase(superAdminEmail)) {
            throw new BusinessException(UserErrorCode.RESERVED_EMAIL);
        }

        // 회원 탈퇴한 이메일로 재가입 시 예외 발생
        // 해당 에러 코드를 프론트로 넘겨 계정 복구 가능
        validateEmailAvailability(request.email());

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
     * 회원 정보를 수정합니다.
     *
     * @param userId  회원 식별자
     * @param request 수정할 회원 프로필 및 비밀번호 정보 DTO
     * @param profile 업로드할 프로필 이미지 스트림 정보 DTO
     * @return 수정 완료된 회원 정보 DTO
     */
    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request, ProfileImageUploadRequest profile) {
        User user = findActiveUser(userId);

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
                        PROFILE_IMAGE_DIR
                );

                user.updateProfileImage(savedFileName);

                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCompletion(int status) {
                                if (status == STATUS_COMMITTED) {
                                    if (oldProfileImageUrl != null && !oldProfileImageUrl.isBlank()) {
                                        storageService.delete(PROFILE_IMAGE_DIR + "/" + oldProfileImageUrl);
                                    }
                                } else if (status == STATUS_ROLLED_BACK) {
                                    storageService.delete(PROFILE_IMAGE_DIR + "/" + savedFileName);
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

            // 비밀번호 검증 (영구 비밀번호 혹은 유효한 임시 비밀번호 중 하나라도 부합하는지 체크)
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
     * 회원을 논리 삭제(탈퇴) 처리하고 세션을 무효화합니다.
     *
     * @param userId 탈퇴할 회원 식별자
     */
    @Override
    @Transactional
    public void softDeleteUser(UUID userId) {
        User user = findUser(userId);

        if (user.getDeletedAt() != null) {
            throw new BusinessException(UserErrorCode.ALREADY_IN_PROGRESS_DELETE);
        }

        // 탈퇴 전, 소속된 공간이 있다면 퇴장 처리
        if (user.getSpace() != null) {
            user.leaveSpace();
        }

        // 유저 논리 삭제
        user.delete();

        // 탈퇴한 유저 세션 만료
        userSessionService.invalidateUserSessions(userId);
    }

    /**
     * 탈퇴 대기 기간(30일)이 만료된 회원들을 일괄 조회하여 물리 삭제(영구 탈퇴) 처리합니다.
     *
     * 개별 유저의 물리 삭제 처리는 독립된 트랜잭션 컴포넌트(UserHardDeleteProcessor)로
     * 위임하여 실행함으로써, 일괄 정리 작업 중 일부 실패가 전체 트랜잭션 롤백으로 이어지지 않도록 방지합니다.
     */
    @Override
    public void deleteExpiredUsers() {
//        OffsetDateTime threshold = OffsetDateTime.now().minusDays(30);
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(1);
        List<User> expiredUsers = userRepository.findAllByDeletedAtBefore(threshold);

        for (User user : expiredUsers) {
            try {
                // 독립적인 트랜잭션에서 개별로 안전하게 영구 삭제 진행
                hardDeleteProcessor.execute(user.getId());
            } catch (Exception e) {
                log.error("[UserService] 만료 유저 물리 삭제 실패 - userId: {}", user.getId(), e);
            }
        }
    }

    /**
     * 논리 삭제 상태인 회원 계정을 30일 이내에 복구합니다.
     *
     * @param email 복구 대상 회원의 이메일
     */
    @Override
    @Transactional
    public void restoreUser(String email) {
        User user = findUserByEmail(email);

        // 탈퇴 상태가 아니거나 이미 30일이 지난 경우 복구할 수 없음.
        if (!user.isRestorable()) {
            throw new BusinessException(UserErrorCode.NOT_ABLE_RESTORE);
        }

        // 복구 처리 (deletedAt = null)
        user.restore();
    }

    /**
     * 존재하는 이메일인지 확인
     */
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(UserErrorCode.NOT_FOUND));
    }

    /**
     * 이메일 존재 여부에 따라 다음과 같이 처리한다.
     * 1. 이메일이 존재하지 않을 경우 회원 가입
     * 2. 이메일이 존재하는 경우
     * - 삭제 시간이 null이 아닌 경우 'ALREADY_IN_PROGRESS_DELETE' 에러
     * - 삭제 시간이 null인 경우 'ALREADY_EXISTS' 에러
     */
    private void validateEmailAvailability(String email) {
        userRepository.findByEmail(email)
                .ifPresent(existingUser -> {
                    if (existingUser.getDeletedAt() != null) {
                        throw new BusinessException(UserErrorCode.ALREADY_IN_PROGRESS_DELETE);
                    }
                    throw new BusinessException(UserErrorCode.ALREADY_EXISTS);
                });
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.NOT_FOUND));
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

    /**
     * 탈퇴한 유저가 아닌지 확인하는 메서드
     */
    private User findActiveUser(UUID userId) {
        User user = findUser(userId);
        if (user.getDeletedAt() != null) {
            throw new BusinessException(UserErrorCode.ALREADY_IN_PROGRESS_DELETE);
        }
        return user;
    }
}
