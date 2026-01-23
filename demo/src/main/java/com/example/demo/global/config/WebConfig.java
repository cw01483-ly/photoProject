package com.example.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 브라우저 요청: /posts/파일명
        // 실제 파일 위치: D:/upload/photoProject/posts/파일명
        registry.addResourceHandler("/posts/**")
                .addResourceLocations("file:/app/upload/posts/");// 배포 경로
                // .addResourceLocations("file:///D:/upload/photoProject/posts/"); 로컬 경로
    }
}
