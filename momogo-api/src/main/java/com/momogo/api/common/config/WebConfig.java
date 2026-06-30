package com.momogo.api.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDirLocation;

    public WebConfig(@Value("${app.file.upload.dir:./uploads}") String uploadDir) {
        Path absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // 크로스 플랫폼 호환성을 보장하기 위한 절대 경로 치환 과정
        String formattedPath = absolutePath.toString().replace("\\", "/");
        if (!formattedPath.endsWith("/")) {
            formattedPath += "/";
        }
        this.uploadDirLocation = "file:" + formattedPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDirLocation);
    }
}
