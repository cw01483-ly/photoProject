package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.dto.UserLoginRequestDto;
import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
// 응답 본문 파싱 후 값 검증용 (AssertJ)

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// MockMvc로 HTTP POST 요청을 만들 때 사용하는 헬퍼 메서드

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// JSON 응답 본문에서 특정 필드 값을 검증할 때 사용하는 유틸리티

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// HTTP 응답 코드(200, 400 등)를 검증할 때 사용하는 유틸리티

/*
    UserControllerTest
    - 실제 HTTP 요청/응답 흐름을 MockMvc를 사용해 테스트하는 클래스
    - UserController의 REST API가 URL, HTTP 메서드, 요청/응답 형식에 맞게 동작하는지 검증
    - 서비스 로직 자체는 UserServiceTest에서 검증했고,
      여기서는 "웹 레이어(컨트롤러 + JSON 매핑 + 시큐리티 + 예외 핸들러)"를 집중적으로 확인
 */
@SpringBootTest //스프링 부트 전체 컨텍스트 로드 (실제 애플리케이션과 거의 동일 환경)
@AutoConfigureMockMvc        // MockMvc 자동 설정 (HTTP 요청/응답을 테스트 코드에서 시뮬레이션)
@Transactional               // 각 테스트 후 DB 롤백 → 테스트 간 데이터 간섭 방지
public class UserControllerTest {

    @Autowired //의존성 주입DI
    private MockMvc mockMvc;
    /*  MockMvc
        - 실제 서버를 띄우지 않고도 스프링 MVC 동작을 테스트할 수 있게 해주는 클래스
        - URL, HTTP 메서드, 헤더, 본문 등을 지정해서 요청을 보내고
          - 응답 코드(status)
          - 응답 JSON 내용(jsonPath)
          을 검증 가능
     */

    @Autowired
    private ObjectMapper objectMapper;
    /*  ObjectMapper
        - 자바 객체 <-> JSON 문자열 변환기
        - 요청 바디로 보낼 DTO를 JSON 문자열로 직렬화할 때 사용
     */

    @Autowired
    private UserRepository userRepository;
    /*  UserRepository
        - 컨트롤러 API 호출 후, 실제 DB에 데이터가 잘 들어갔는지/삭제되었는지
          추가로 확인하고 싶을 때 사용
     */

    // ⭐ 회원가입 성공 테스트 (POST /api/users)
    @Test
    @DisplayName("회원가입 성공 : 올바른 요청 JSON 보내면 201과 UserResponseDto 반환")
    void register_success() throws Exception {
        // [GIVEN] 회원가입 요청 DTO 생성
        UserSignupRequestDto requestDto = UserSignupRequestDto.builder()
                .username("controller1")
                .password("Password1@")
                .nickname("닉네임1")
                .email("example@example.com")
                .build();

        // DTO -> JSON 문자열 변환
        String json = objectMapper.writeValueAsString(requestDto);

        // [WHEN] MockMvc를 사용해 Post /api/users 요청 전송
        var resultAction = mockMvc.perform(
                post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON) // 요청 본문 타입 application/json
                        .content(json)); // JSON본문

        // [THEN] HTTP 응답 코드 및 응답 JSON 구조 검증
        resultAction
                .andExpect(status().isCreated())// 201 Created 이어야 함 (ResponseEntity.created(...) 사용)
                // ApiResponse 구조: { "success": true, "data": { ... }, "message": "회원가입 성공" }
                .andExpect(jsonPath("$.success").value(true))  // success 필드 true
                .andExpect(jsonPath("$.message").value("회원가입 성공"))// 메시지 검증
                .andExpect(jsonPath("$.data.username").value("controller1"))
                // username 소문자 변환 가정
                .andExpect(jsonPath("$.data.email").value("example@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("닉네임1"));

        // 추가 검증 ) DB에 해당 사용자 저장되었는지 확인
        boolean exists = userRepository.findByUsername("controller1").isPresent();
        assertThat(exists).isTrue(); //DB에 유저 존재해야 테스트 성공
    }
}
