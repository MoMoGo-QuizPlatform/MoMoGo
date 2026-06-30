package com.momogo.core.domain.user.mapper;

import com.momogo.core.domain.user.dto.response.UserResponse;
import com.momogo.core.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Value;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class UserMapper {

    @Value("${app.file.base-url}")
    protected String fileBaseUrl;

    @Mapping(target = "banned", source = "isBanned")
    @Mapping(target = "profileImageUrl", source = "profileImageUrl", qualifiedByName = "resolveImageUrl")
    public abstract UserResponse toResponse(User user);

    // 이미지 파일명을 클라이언트가 접근할 수 있는 완전한 URL 형태로 변환합니다.
    @Named("resolveImageUrl")
    protected String resolveImageUrl(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }
        if (imageName.startsWith("http://") || imageName.startsWith("https://")) {
            return imageName;
        }
        return fileBaseUrl + "/profile/" + imageName;
    }
}
