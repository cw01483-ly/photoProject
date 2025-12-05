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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.domain.user.entity.User;
import org.springframework.security.test.context.support.WithMockUser; // @WithMockUser
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // get() 헬퍼



import static org.assertj.core.api.Assertions.assertThat;
// 응답 본문 파싱 후 값 검증용 (AssertJ)

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// MockMvc로 HTTP POST 요청을 만들 때 사용하는 헬퍼 메서드

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

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



    // ⭐ 로그인 성공 테스트 (POST /api/users/login)
    @Test
    @DisplayName("로그인 성공 : 올바른 username,pw 로그인 시 200과 UserResponseDto 반환")
    void login_success() throws Exception {
        // [GIVEN] 1) 회원가입으로 유저 생성
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username("loginuser1")
                .password("Password123!")
                .nickname("닉네임1")
                .email("example@example.com")
                .build();

        // 평문 비밀번호 인코딩
        String rawPw = signupRequest.getPassword();
        String encodedPw = passwordEncoder.encode(rawPw); //인코딩

        // 서비스 레이어 호출하여 가입처리 (DB저장)
        var savedUser = userRepository.save(
                com.example.demo.domain.user.entity.User.builder()
                        .username(signupRequest.getUsername().toLowerCase().trim()) // 서비스 로직과 동일하게 표준화 가정
                        .password(encodedPw)
                        .nickname(signupRequest.getNickname())
                        .email(signupRequest.getEmail().toLowerCase().trim())
                        .build()
        );

        // 2) 로그인 요청 DTO 생성 (username + 평문 pw)
        UserLoginRequestDto loginRequest = UserLoginRequestDto.builder()
                .username("loginuser1")
                .password("Password123!")
                .build();
        // DTO -> JSON 문자열 변환
        String loginJson = objectMapper.writeValueAsString(loginRequest);

        // [WHEN] POST /api/users/login 요청 전송
        var resultAction = mockMvc.perform(
                post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson));

        // [THEN] 응답 상태 및 JSON 내용 검증
        resultAction
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.username").value("loginuser1"))
                .andExpect(jsonPath("$.data.email").value("example@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("닉네임1"));
    }

    /*
        ※ 추후 확장
        - 회원가입 실패 : 잘못된 이메일 형식 / 공백 username 등 → 400 + GlobalExceptionHandler 응답 구조 검증
        - 단일 조회(GET /api/users/{id}) : ADMIN 또는 본인일 때 200, 타인일 때 403
        - 닉네임/이메일 수정 PATCH API : 성공/실패, 권한(본인/관리자) 체크
        - 삭제 DELETE /api/users/{id} : 성공 케이스, 없는 ID → 404, 권한 없는 사용자 → 403
     */


    // ⭐ 단일 조회 성공 테스트
    @Test
    @DisplayName("단일 조회 성공 : 관리자 GET /api/users/{id} 호출 시 200과 UserResponseDto 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getUserById_success_asAdmin() throws Exception {
        // [GIVEN] 조회 대상 사용자 1명 DB 생성
        User user = userRepository.save(
                User.builder()
                        .username("getbyiduser1")
                        .password(passwordEncoder.encode("Password123!"))
                        .nickname("조회대상")
                        .email("example@example.net")
                        .build()
        );

        // [WHEN] GET /api/users/{id} 요청 전송 (관리자 권한)
        var  resultAction = mockMvc.perform(
                get("/api/users/{id}", user.getId())
                        .accept(MediaType.APPLICATION_JSON));

        // [THEN] 응답 코드 및 JSON 내용 검증
        resultAction
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("단일 사용자 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.username").value("getbyiduser1"))
                .andExpect(jsonPath("$.data.email").value("example@example.net"))
                .andExpect(jsonPath("$.data.nickname").value("조회대상"));
    }


    // ⭐ 전체 사용자 조회 테스트 ( GET / api/users) - 관리자 권한 성공 케이스
    @Test
    @DisplayName("전체 조회 성공 : 관리자가 GET /api/users 호출 시 200과 사용자 목록 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAll_success_asAdmin() throws Exception {
        //  테스트를 깨끗한 상태에서 시작
        //userRepository.deleteAll(); < 코드500 반환 후보라 제거

        // [GIVEN] 1) 조회 대상 사용자 2명 생성 (전체 조회 2명 이상)
        User user1 = userRepository.save(
                User.builder()
                        .username("listuser1")
                        .password(passwordEncoder.encode("Password1!")) // 비밀번호는 그냥 인코딩해서 넣음
                        .nickname("리스트닉1")
                        .email("list1@example.com")
                        .build()
        );
        User user2 = userRepository.save(
                User.builder()
                        .username("listuser2")
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("리스트닉2")
                        .email("list2@example.com")
                        .build()
        );

        // [WHEN] 2) 관리자 권한으로 GET /api/users 요청, body가 없는 GET 요청 >> .content() 필요 없음
        var  resultAction = mockMvc.perform(
                get("/api/users") // GET /api/users
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());// 콘솔창에 조회한 데이터 출력

        // [THEN] 3) 응답 상태 코드와 JSON 내용 검증, ApiResponse<List<UserResponseDto>> 형태를 기대
        resultAction
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success").value(true)) // success == true
                .andExpect(jsonPath("$.message").value("전체 사용자 조회 성공"))
                .andExpect(jsonPath("$.data").isArray()); // data가 배열형태로 왔는지 확인
        resultAction
                .andExpect(jsonPath("$.data[?(@.username == 'listuser1')]").exists())
                .andExpect(jsonPath("$.data[?(@.username == 'listuser2')]").exists());
        // 배열의 순서 상관 없이 username이 포함되어있으면 성공
    }



    // ⭐ username 기준 조회 테스트 ( GET /api/users/username/{username} ) - 관리자 권한 성공 케이스
    @Test
    @DisplayName("username 조회 성공 : 관리자가 GET /api/users/username/{username} 호출 시 200과 UserResponseDto 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getByUsername_success_asAdmin() throws Exception {

        // [GIVEN] 조회 대상 사용자 생성
        User saved =  userRepository.save(
                User.builder()
                        .username("finduser1")
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("조회닉네임")
                        .email("find@example.com")
                        .build()
        );

        // [WHEN] GET /api/users/username/{username} 요청 전송 (관리자 권한)
        var resultAction = mockMvc.perform(
                get("/api/users/username/{username}", "finduser1") //URL 경로 변수 username 전달
                        .accept(MediaType.APPLICATION_JSON) //JSON 응답 기대
        ).andDo(print()); // 응답 전체 출력

        // [THEN] 응답 코드 및 JSON 내용 검증
        resultAction
                .andExpect(status().isOk()) // HTTP 200 정상 반환
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("username 기준 조회 성공"))
                .andExpect(jsonPath("$.data.username").value("finduser1"))
                // ↑반환된 DTO의 username 값이 저장한 값과 일치하는지 확인
                .andExpect(jsonPath("$.data.email").value("find@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("조회닉네임"));

    }
}
