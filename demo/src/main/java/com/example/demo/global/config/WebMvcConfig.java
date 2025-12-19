package com.example.demo.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration //  이 클래스가 스프링 설정 클래스임을 의미
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir:D:/upload/photoProject}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/images/**") // 브라우저에서 접근할 URL 패턴: /images/...
                .addResourceLocations("file:" + ensureTrailingSlash(uploadDir));
        // D:/upload/photoProject/ (설정값)로 매핑

    }

    // file: 경로는 끝에 "/"가 있으면 디렉토리 인식이 확실해서 보정
    private String ensureTrailingSlash(String path) {
        if (path == null || path.isBlank()) {
            return "D:/upload/photoProject/";
        }
        return path.endsWith("/") ? path : path + "/";
    }
}
