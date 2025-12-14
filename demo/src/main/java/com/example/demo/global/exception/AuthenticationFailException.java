package com.example.demo.global.exception;

/*
  로그인 인증 실패 전용 예외
  - 아이디/비밀번호 불일치 등 인증 실패 상황에서 사용
  - GlobalExceptionHandler에서 401(UNAUTHORIZED)로 처리됨
 */
public class AuthenticationFailException extends RuntimeException {

    public AuthenticationFailException(String message) {
        super(message);
    }
}