package com.momogo.api.user;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;
import com.momogo.core.domain.user.dto.request.UserCreateRequest;
import com.momogo.core.domain.user.dto.request.UserBannedRequest;
import com.momogo.core.domain.user.dto.request.UserPageRequest;
import com.momogo.core.domain.user.dto.request.UserUpdateRequest;
import com.momogo.core.domain.user.dto.response.CursorResponse;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 신규 회원을 생성(회원가입)합니다.
     *
     * @param request 회원가입 요청 DTO
     * @return 생성된 회원 정보 DTO
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserResponse userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    /**
     * 로그인된 현재 유저의 상세 정보를 조회합니다.
     *
     * @param userDetails 인증 객체에서 추출한 현재 유저 상세 정보
     * @return 현재 유저 정보 DTO
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> findUser(
            @AuthenticationPrincipal MoMoGoUserDetails userDetails
    ) {
        return ResponseEntity.ok(userDetails.getUserResponse());
    }

    /**
     * 로그인된 현재 유저의 프로필(이름, 비밀번호, 프로필 이미지 등)을 수정합니다.
     *
     * @param userId  인증 객체에서 추출한 유저 식별자
     * @param request 수정할 프로필 정보 DTO
     * @param profile 업로드할 프로필 이미지 파일
     * @return 수정 완료된 회원 정보 DTO
     * @throws IOException 이미지 파일 스트림 처리 중 예외 발생 시
     */
    @PatchMapping(
            value = "/me",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal(expression = "userResponse.id") UUID userId,
            @RequestPart(value = "data", required = false) @Valid UserUpdateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile profile
    ) throws IOException {
        ProfileImageUploadRequest uploadRequest = null;
        if (profile != null && !profile.isEmpty()) {
            uploadRequest = new ProfileImageUploadRequest(
                    profile.getInputStream(),
                    profile.getOriginalFilename(),
                    profile.getContentType(),
                    profile.getSize()
            );
        }

        UserResponse userResponse = userService.updateUser(userId, request, uploadRequest);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * 로그인된 현재 유저의 계정을 탈퇴(논리 삭제) 처리합니다.
     *
     * @param userId 인증 객체에서 추출한 유저 식별자
     * @return 응답 본문이 없는 ResponseEntity (HTTP 204 No Content)
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(
            @AuthenticationPrincipal(expression = "userResponse.id") UUID userId
    ) {
        userService.softDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 전체 유저 목록을 페이징(커서 기반) 조회합니다. (SUPER_ADMIN 전용)
     *
     * @param request 검색 조건 및 페이징 설정 DTO
     * @return 커서 정보가 포함된 페이징된 유저 목록 응답 객체
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CursorResponse<UserResponse>> findAllUsers(
            @Valid @ModelAttribute UserPageRequest request
    ) {
        return ResponseEntity.ok(userService.findAllUsers(request));
    }

    /**
     * 특정 유저의 정지(밴) 상태를 업데이트합니다. (SUPER_ADMIN 전용)
     *
     * @param userId  정지 상태를 변경할 대상 유저 식별자
     * @param request 정지 여부(banned) 설정 DTO
     * @return 변경 완료된 회원 정보 DTO
     */
    @PatchMapping("/{userId}/banned")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserResponse> updateUserBannedStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserBannedRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserBannedStatus(userId, request.banned()));
    }
}
