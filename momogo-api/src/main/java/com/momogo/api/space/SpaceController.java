package com.momogo.api.space;

import com.momogo.api.auth.details.MoMoGoUserDetails;
import com.momogo.core.domain.space.dto.request.SpaceCreateRequest;
import com.momogo.core.domain.space.dto.request.SpaceCursorRequest;
import com.momogo.core.domain.space.dto.request.SpaceJoinRequest;
import com.momogo.core.domain.space.dto.request.SpaceUpdateRequest;
import com.momogo.core.domain.space.dto.request.SpaceUserRoleUpdateRequest;
import com.momogo.core.domain.space.dto.response.SpaceCursorPaginationResponse;
import com.momogo.core.domain.space.dto.response.SpaceResponse;
import com.momogo.core.domain.space.entity.Space;
import com.momogo.core.domain.space.mapper.SpaceMapper;
import com.momogo.core.domain.space.service.SpaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceController {

  private final SpaceService spaceService;
  private final SpaceMapper spaceMapper;

  /**
   * 공간 개설
   * @param userDetails 유저 정보
   * @param request 공간 개설 요청 DTO
   * @return 개설된 공간 정보
   */
  @PostMapping
  public ResponseEntity<SpaceResponse> createSpace(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @Valid @RequestBody SpaceCreateRequest request
  ) {
    Space space = spaceService.createSpace(userDetails.getUserResponse().id(), request);
    SpaceResponse response = spaceMapper.toResponse(space);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * 비밀번호 기반 공간 입장
   * @param userDetails 유저 아이디
   * @param spaceId 공간 아이디
   * @param request 공간 입장 요청 DTO
   * @return 입장 성공 여부
   */
  @PostMapping("/{spaceId}/join")
  public ResponseEntity<Void> joinSpace(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID spaceId,
      @Valid @RequestBody SpaceJoinRequest request
  ) {
    spaceService.joinSpace(userDetails.getUserResponse().id(), spaceId, request.spacePassword());
    return ResponseEntity.ok().build();
  }

  /**
   * 내가 현재 소속된 공간 정보 단건 조회
   * @param userDetails 유저 아이디
   * @return 내가 소속된 공간 정보
   */
  @GetMapping("/me")
  public ResponseEntity<SpaceResponse> getMySpace(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails
  ) {
    return spaceService.getMySpace(userDetails.getUserResponse().id())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * 커서 페이지네이션을 활용한 공간 리스트 반환
   * @param userDetails 유저 아이디
   * @param request 커서 요청 DTO
   * @return 공간 리스트
   */
  @GetMapping("/unjoined")
  public ResponseEntity<SpaceCursorPaginationResponse<SpaceResponse>> getUnjoinedSpaces(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @ModelAttribute SpaceCursorRequest request
  ) {

    // 1. 단 한 번만 파싱 및 검증 수행
    SpaceCursorRequest.ParsedCursor parsed = request.parse();

    // 2. 서비스 호출
    SpaceCursorPaginationResponse<Space> cursorPaginationResponse = spaceService.getUnjoinedSpacesByCursor(
        userDetails.getUserResponse().id(), parsed.lastSpaceId(), parsed.lastCreatedAt(), request.size()
    );

    // 내부 Entity 리스트를 DTO 리스트 변환
    List<SpaceResponse> responseList =  spaceMapper.toResponseList(cursorPaginationResponse.values());
    SpaceCursorPaginationResponse<SpaceResponse> response = new SpaceCursorPaginationResponse<>(
        responseList,
        cursorPaginationResponse.hasNext(),
        cursorPaginationResponse.nextCursor()
    );

    return ResponseEntity.ok(response);
  }

  /**
   * 개설한 공간의 이름, 설명, PW, 이미지 수정
   * @param userDetails 유저 아이디
   * @param spaceId 공간 아이디
   * @param request 공간 수정 요청 DTO
   * @return 수정된 공간 정보
   */
  @PutMapping("/{spaceId}")
  public ResponseEntity<SpaceResponse> updateSpace(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID spaceId,
      @Valid @RequestBody SpaceUpdateRequest request
  ) {
    Space space = spaceService.updateSpace(userDetails.getUserResponse().id(), spaceId, request);
    SpaceResponse response = spaceMapper.toResponse(space);
    return ResponseEntity.ok(response);
  }

  /**
   * 공간 삭제
   * @param userDetails 유저 아이디
   * @param spaceId 공간 아이디
   * @return 삭제 성공 여부
   */
  @DeleteMapping("/{spaceId}")
  public ResponseEntity<Void> deleteSpace(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID spaceId
  ) {
    spaceService.deleteSpace(userDetails.getUserResponse().id(), spaceId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 공간 멤버 권한 변경 (USER <-> ADMIN)
   * @param userDetails 유저 아이디
   * @param spaceId 공간 아이디
   * @param targetUserId 대상 유저 아이디
   * @param request 권한 변경 요청 DTO
   * @return 권한 변경 성공 여부
   */
  @PatchMapping("/{spaceId}/users/{userId}/role")
  public ResponseEntity<Void> changeUserRole(
      @AuthenticationPrincipal MoMoGoUserDetails userDetails,
      @PathVariable UUID spaceId,
      @PathVariable("userId") UUID targetUserId,
      @Valid @RequestBody SpaceUserRoleUpdateRequest request
  ) {
    spaceService.changeUserRole(userDetails.getUserResponse().id(), spaceId, targetUserId, request.userRole());
    return ResponseEntity.ok().build();
  }
}


