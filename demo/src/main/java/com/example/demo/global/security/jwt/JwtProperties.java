package com.example.demo.global.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")

// 스프링 yml값 자동 주입 용도
public class JwtProperties {

    private String secret;
    private int accessTokenExpMinutes;
    private String cookieName;
}
