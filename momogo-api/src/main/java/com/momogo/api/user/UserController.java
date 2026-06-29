package com.momogo.api.user;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.core.domain.user.dto.request.ProfileImageUploadRequest;
import com.momogo.core.domain.user.dto.request.UserCreateRequest;
import com.momogo.core.domain.user.dto.request.UserUpdateRequest;
import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        UserResponse userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> findUser(
            @AuthenticationPrincipal MoMoGoUserDetails userDetails
    ) {
        return ResponseEntity.ok(userDetails.getUserResponse());
    }

    @PatchMapping(
            value = "/me",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal MoMoGoUserDetails userDetails,
            @RequestPart(value = "data", required = false) @Valid UserUpdateRequest request,
            @RequestPart(value = "file", required = false) MultipartFile profile
    ) throws IOException {
        UUID currentUserId = userDetails.getUserResponse().id();

        ProfileImageUploadRequest uploadRequest = null;
        if (profile != null && !profile.isEmpty()) {
            uploadRequest = new ProfileImageUploadRequest(
                    profile.getInputStream(),
                    profile.getOriginalFilename(),
                    profile.getContentType(),
                    profile.getSize()
            );
        }

        UserResponse userResponse = userService.updateUser(currentUserId, request, uploadRequest);
        return ResponseEntity.ok(userResponse);
    }
}
