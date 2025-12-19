package com.example.demo.global.exception;


import com.example.demo.domain.user.dto.UserLoginRequestDto;
import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


/*
    GlobalExceptionHandler + 로그인 실패 테스트 (비밀번호 불일치)
    목적:
      - /api/users/login 으로 로그인 요청 시 잘못된 비밀번호를 넣으면
        UserService.login() 이 IllegalArgumentException()을 던지고,
        GlobalExceptionHandler.handleIllegalArgumentException() 이 이를 잡아서
        HTTP 400 + ApiResponse<ErrorResponse> JSON 응답을 보내는지 검증.
*/

/*  GlobalExceptionHandler 테스트
    1) 로그인 실패 - 비밀번호 불일치
    2) 회원가입 실패 - DTO 검증 실패(@Valid)
*/
@SpringBootTest // 스프링부트 실제 환경 테스트
@AutoConfigureMockMvc /* MockMvc 자동 생성
Spring MVC 애플리케이션의 컨트롤러(Controller) 계층을 실제 HTTP 요청 없이 테스트*/
@Transactional // 각 테스트 이후 DB 롤백
public class GlobalExceptionHandlerTest {

    @Autowired /* 의존성 주입(DI)
 두 객체 사이의 관계(의존 관계)를 객체 내부가 아닌 외부(Spring 컨테이너)에서 설정 */
    private MockMvc mockMvc; // 가짜 HTTP 요청보내는 도구

    @Autowired
    private ObjectMapper objectMapper; // DTO -> JSON 변환기

    @Autowired
    private UserService userService; // 회원가입 테스트 준비

    @Test // ⭐ 비밀번호 불일치 시 에러응답 테스트
    @DisplayName("로그인 실패 : 비밀번호 불일치 시 401과 에러 응답 반환")
    void login_fail_wrongPw_returnBadRequest() throws Exception{

        // [GIVEN] 1) 정상 회원가입
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username("testUser1")
                .password("CorrectPw1!")
                .nickname("예외테스트")
                .email("example@example.com")
                .build();
        userService.register(signupRequest); //DB 유저 저장

        // 2) 틀린 비밀번호로 로그인 요청 DTO 생성
        UserLoginRequestDto wrongPwLoginRequest = UserLoginRequestDto.builder()
                .username("testUser1")
                .password("WrongPw1!") //틀린 Pw
                .build();

        // 3) DTO -> JSON 문자열 변환
        String requestJson = objectMapper.writeValueAsString(wrongPwLoginRequest);

        // [WHEN & THEN]
        mockMvc.perform(// mockMvc.perform(...) :가짜 HTTP 요청을 컨트롤러에 직접 보내는 기능

                post("/api/users/login")
                // ↑ UserController @RequestMapping("/api/users") + @PostMapping("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                // ↑ 지금 보내는 요청 바디는 JSON임을 선언, 실제 HTTP 요청의 헤더 중 Content-Type: application/json 과 동일.
                        .content(requestJson)
                // ↑ HTTP 요청의 Body 부분. 만든 JSON 문자열(requestJson)을 그대로 Body에 넣음
        )
                /* ↓ perform() 결과가 컨트롤러 + 서비스 + 예외처리까지
                     전부 실행한 결과가 아래 조건을 만족하는지 확인*/
                .andExpect(status().isUnauthorized()) // 401 Unauthorized
                .andExpect(jsonPath("$.success").value(false))
                /*응답 JSON의 "success" 필드를 꺼내서 false인지 검사
                    "$" = JSON의 루트(root)
                    "$.success" = 루트 아래 success 값
                    ApiResponse.fail(...) 로 만들어진 JSON 구조를 그대로 검사
                */
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호를 확인해주세요."))
                /*jsonPath("$.message").value("아이디 또는 비밀번호를 확인해주세요.")
                    JSON에서 "message" 필드를 꺼내 원하는 문자열인지 확인
                    이 메시지는 UserService.login()에서 던진 IllegalArgumentException 메시지가
                    GlobalExceptionHandler에서 그대로 전달된 값
                */
                .andExpect(jsonPath("$.data").exists());// data(ErrorResponse)가 존재해야 함
                /*jsonPath("$.data").exists()
                    JSON에서 "data" 라는 필드가 존재하는지 검사
                    data 필드에는 ErrorResponse 객체가 들어있고
                    ErrorResponse에는 success=false, status=400, path="/api/users/login" 등이 들어 있음.
                    ★구조까지 전부 검사하진 않지만 "data" 필드가 있다 = 예외 처리 정상 작동했다는 증거.

                */
    }

    @Test // ⭐ 회원가입 검증 실패테스트
    @DisplayName("회원가입 실패 : DTO 검증 실패 시 400과 에러 응답 반환")
    void signup_fail_validation_returnBadRequest() throws Exception{

        // [GIVEN] 1) 잘못된 회원가입 요청 DTO
        UserSignupRequestDto invalidSignupRequest = UserSignupRequestDto.builder()
                .username("") //빈 문자열 @NotBlank검증
                .password("pw") // @Size 검증
                .nickname("") // 공백검증
                .email("not-an-email") // @Email 형식 검증
                .build();

        // 2) DTO -> JSON 문자열 반환
        String requestJson = objectMapper.writeValueAsString(invalidSignupRequest);

        // [WHEN & THEN]
        // 회원가입 엔드포인트는 @RequestMapping("/api/users") + @PostMapping(빈 문자열)
        // -> 실제 URL = POST /api/users
        mockMvc.perform(
                post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        // ↑ 지금 보내는 요청 바디는 JSON임을 선언
                        .content(requestJson)
                        // ↑ HTTP 요청의 Body 부분
        )
                // 검사1)상태코드 400(MethodArgumentNotValidException -> handleValidationException)
                .andExpect(status().isBadRequest())
                // 검사2)공통 응답 포맷 상 success == false
                .andExpect(jsonPath("$.success").value(false))
                // 검사3)에러 메시지 존재하는지만 확인(구체 문구는 DTO 설정에 따라 달라질 수 있음)
                .andExpect(jsonPath("$.message").exists())
                // 검사4)data(ErrorResponse) 객체가 존재
                .andExpect(jsonPath("$.data").exists())
                // 검사5) ErrorResponse 내부 status == 400
                .andExpect(jsonPath("$.data.status").value(400))
                // 검사6) ErrorResponse 내부 path == "/api/users"
                .andExpect(jsonPath("$.data.path").value("/api/users"));
    }
}
