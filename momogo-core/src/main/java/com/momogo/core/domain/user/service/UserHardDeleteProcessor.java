package com.momogo.core.domain.user.service;

import com.momogo.core.common.exception.BusinessException;
import com.momogo.core.common.storage.StorageService;
import com.momogo.core.common.util.UrlUtils;
import com.momogo.core.domain.user.entity.User;
import com.momogo.core.domain.user.exception.UserErrorCode;
import com.momogo.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * 회원 물리 삭제(영구 탈퇴) 처리를 전담하는 독립 트랜잭션 컴포넌트입니다.
 *
 * 각 회원 삭제 시 독립적인 새로운 트랜잭션(REQUIRES_NEW)을 실행하여,
 * 한 회원의 삭제 실패(예: 외래 키 제약 예외)가 다른 회원의 일괄 물리 삭제(배치 작업)
 * 전체 롤백으로 전파되는 것을 차단합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserHardDeleteProcessor {

    private static final String PROFILE_IMAGE_DIR = "profile";

    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.NOT_FOUND));

        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.isBlank()
                && !UrlUtils.isExternalUrl(user.getProfileImageUrl())) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_COMMITTED) {
                                try {
                                    storageService.delete(PROFILE_IMAGE_DIR + "/" + profileImageUrl);
                                } catch (Exception e) {
                                    log.error("[UserHardDeleteProcessor] 회원 탈퇴 프로필 이미지 삭제 실패 - userId: {}, file: {}", userId, profileImageUrl, e);
                                }
                            }
                        }
                    }
            );
        }
        // DB 유저 데이터 물리 삭제
        userRepository.delete(user);
    }
}