package com.example.demo.global.security.jwt.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")

// 스프링 yml값 자동 주입 용도
public class JwtProperties {

    // Access Token
    private String secret; // JWT 서명용 시크릿 키
    private int accessTokenExpMinutes; // Access Token 만료 시간(분)
    private String cookieName; // Access Token 쿠키 이름

    // Refresh Token
    private String refreshCookieName; // Refresh Token 쿠키 이름
    private int refreshTokenExpDays; // Refresh Token 만료 시간(일)
}
