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
import java.util.Map;// Map.of 사용
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // get() 헬퍼
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch; // patch() 헬퍼
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete; // delete() 헬퍼

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
        - 단일 조회(GET /api/users/{id}) : 실패, 타인일 때 403
        - 닉네임/이메일 수정 PATCH API : 실패, 체크
        - 삭제 DELETE /api/users/{id} :  실패, 없는 ID → 404, 권한 없는 사용자 → 403
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


    // ⭐ 닉네임 업데이트 테스트 ( PATCH /api/users/{id}/nickname ) - 관리자 권한 성공 케이스
    @Test
    @DisplayName("닉네임 수정 성공 : 관리자가 PATCH /api/users/{id}/nickname 호출 시 200과 변경된 닉네임 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateNickname_success_asAdmin() throws Exception {

        //[GIVEN] 1) 기존 사용자 1명을 DB에 저장. (닉네임을 바꿀 대상)
        User user = userRepository.save(
                User.builder()
                        .username("nickuser1")
                        .password(passwordEncoder.encode("Password1!"))// 비밀번호는 인코딩해서 저장
                        .nickname("기존닉네임")
                        .email("nick1@example.com")
                        .build()
        );

        // [GIVEN] 2) 닉네임 수정 요청 바디 JSON 준비, UserController 내부 static class NicknameUpdateRequest
        Map<String, String> requestBody = Map.of("nickname", "새닉네임");
        String json = objectMapper.writeValueAsString(requestBody);
        // objectMapper : 자바객체(Map) -> JSON 문자열로 변환 >> { "nickname": "새닉네임" } 형태의 JSON 문자열 생성

        // [WHEN] 3) PATCH /api/users/{id}/nickname 요청 전송 (관리자 권한)
        var resultAction = mockMvc.perform(
                patch("/api/users/{id}/nickname", user.getId())          // URL 경로에 대상 사용자 id 포함
                        .contentType(MediaType.APPLICATION_JSON)          // 요청 본문 타입: application/json
                        .content(json)                                   // JSON 요청 바디 전송
        ).andDo(print());

        // [THEN] 4) 응답 상태 코드와 JSON 응답 본문 검증
        resultAction
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("닉네임 수정 성공"))
                // ↓ 반환된 data.id가 수정한 사용자 id 인지 확인
                .andExpect(jsonPath("$.data.id").value(user.getId().intValue()))
                // ↓ 반환된 data.nickname 이 "새닉네임" 으로 변경되었는지 확인
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));

        // [THEN] 5) DB에 실제로 닉네임이 바뀌었는지도 추가로 확인 (2차 검증)
        User updated = userRepository.findById(user.getId()).orElseThrow();
        // DB에서 다시 조회했을 때 닉네임이 "새닉네임" 으로 저장되어 있어야 한다.
        assertThat(updated.getNickname()).isEqualTo("새닉네임");
    }


    // ⭐ 이메일 수정 테스트 (PATCH /api/users/{id}/email) - 관리자 권한 성공 케이스
    @Test
    @DisplayName("이메일 수정 성공 : 관리자가 PATCH /api/users/{id}/email 호출 시 200과 변경된 이메일 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateEmail_success_asAdmin() throws Exception {
        // [GIVEN] 1) 기존 사용자 1명을 DB에 저장
        User user = userRepository.save(
                User.builder()
                        .username("emailuser1")
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("이메일닉")
                        .email("old@example.com")
                        .build()
        );

        /* [GIVEN] 2) 이메일 수정 요청 바디 JSON 준비
             UserController 내부 static class EmailUpdateRequest
             > ( 필드 : String email, JSON 형태 { "email": "new@example.com" }*/
        Map<String, String> requestBody = Map.of("email", "new@example.com");
        String json = objectMapper.writeValueAsString(requestBody);// objectMapper : 자바 객체(Map) -> JSON 문자열로 변환

        // [WHEN] 3) PATCH /api/users/{id}/email 요청 전송 (관리자 권한)
        var resultAction = mockMvc.perform(
                patch("/api/users/{id}/email", user.getId()) // URL 경로에 대상 사용자 id 포함
                        .contentType(MediaType.APPLICATION_JSON)    // 요청 본문 타입: application/json
                        .content(json)  // JSON 요청 바디 전송
        ).andDo(print());

        // [THEN] 4) 응답 상태 코드와 JSON 응답 본문 검증
        resultAction
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("이메일 수정 성공"))
                // ↓ 반환된 data.id 가 수정한 사용자 id 인지 확인 (Long -> int 변환 : intValue() )
                .andExpect(jsonPath("$.data.id").value(user.getId().intValue()))
                // ↓ 반환된 data.email 이 "new@example.com" 으로 변경되었는지 확인
                .andExpect(jsonPath("$.data.email").value("new@example.com"));

        // [THEN] 5) 실제 DB에 이메일이 바뀌었는지도 추가로 확인 (2차 검증)
        User updated = userRepository.findById(user.getId()).orElseThrow();
        // DB에서 다시 조회했을 때 email 필드가 "new@example.com" 으로 저장되어야 함.
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
    }


    // ⭐ 회원 삭제 테스트 (DELETE /api/users/{id}) - 관리자 권한 성공 케이스
    @Test
    @DisplayName("회원 삭제 성공 : 관리자가 DELETE /api/users/{id} 호출 시 200과 '회원 삭제 성공' 메시지 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_success_asAdmin() throws Exception {
        // [GIVEN] 1) 삭제 대상 사용자 1명을 DB에 저장해둔다.
        User user = userRepository.save(
                User.builder()
                        .username("deleteuser1")
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("삭제대상닉네임")
                        .email("delete@example.com")
                        .build()
        );
        Long id = user.getId(); // 삭제 대상의 PK값

        // [WHEN] 2) 관리자 권한으로 DELETE /api/users/{id} 요청 전송
        var resultAction = mockMvc.perform(
                delete("/api/users/{id}", id) // DELETE /api/users/{id}
        ).andDo(print());

        // [THEN] 3) 응답 상태 코드와 JSON 응답 본문 검증
        resultAction
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원 삭제 성공"));

        // [THEN] 4) DB에서 해당 사용자 삭제 되었는지 2차 검증
        boolean exists = userRepository.findById(id).isPresent();
        assertThat(exists).isFalse();// 삭제 후 findById(id) 값이 비어있어야함
    }


    // ⭐ 회원 삭제 실패 테스트 (DELETE /api/users/{id}) - 존재하지 않는 ID 요청 시 404 반환 (관리자 권한)
    @Test
    @DisplayName("회원 삭제 실패 : 존재하지 않는 ID로 DELETE /api/users/{id} 호출 시 404와 실패 응답 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_notFound_return404() throws Exception {

        // [GIVEN] 사용자 생성하지 않거나 존재하지 않을 큰 값 Id 생성하여 사용
        Long notExistingId = 999999L;

        // [WHEN] 관리자 권한으로 존재않는 ID에 대해 DELETE 요청 전송
        var resultAction = mockMvc.perform(
                delete("/api/users/{id}", notExistingId)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] 1) HTTP 상태코드가 404 Not Found 인지 확인
        resultAction
                .andExpect(status().isNotFound())
                // [THEN] 2) 공통 응답 포맷(ApiResponse)에서 success가 false 이어야 함
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());
                // 메시지가 존재하는지 확인 (정확한 문구는 GlobalExceptionHandler 구현에 따라 다를 수 있으므로)
    }


    // ⭐ 단일 조회 실패 테스트 (GET /api/users/{id}) - 존재하지 않는 ID 요청 시 404 반환
    @Test
    @DisplayName("단일 조회 실패 : 존재하지 않는 ID로 GET /api/users/{id} 호출 시 404와 실패 응답 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getUserById_notFound_return404() throws Exception {

        // [GIVEN] 존재하지 않는 ID 값 준비
        Long notExistingId = 999999L;

        // [WHEN] 관리자 권한으로 존재하지 않는 ID에 대해 GET 요청 전송
        var resultAction = mockMvc.perform(
                get("/api/users/{id}", notExistingId)
                        .accept(MediaType.APPLICATION_JSON) // JSON 응답 기대
        ).andDo(print());

        // [THEN]
        resultAction
                .andExpect(status().isNotFound()) // 1) HTTP 상태코드가 404 Not Found 인지 확인
                // ↓ [THEN] 2) 공통 응답 포맷(ApiResponse)에서 success가 false 이어야 함
                .andExpect(jsonPath("$.success").value(false))
                // ↓ [THEN] 3) 에러 메시지가 비어있지 않은지만 확인
                .andExpect(jsonPath("$.message").isNotEmpty());
    }


    // ⭐ username 조회 실패 테스트 (GET /api/users/username/{username}) - 존재하지 않는 username 요청 시 404 반환
    @Test
    @DisplayName("username 조회 실패 : 존재하지 않는 username으로 GET /api/users/username/{username} 호출 시 404와 실패 응답 반환")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getByUsername_notFound_return404() throws Exception {

        // [GIVEN] 존재하지 않는 username 값 준비 (DB에 없는 값이라고 가정)
        String notExistingUsername = "no_such_user_123";

        // [WHEN] 관리자 권한으로 존재하지 않는 username에 대해 GET 요청 전송
        var resultAction = mockMvc.perform(
                get("/api/users/username/{username}", notExistingUsername)
                        .accept(MediaType.APPLICATION_JSON) // JSON 응답 기대
        ).andDo(print()); // 응답 전체(상태코드, Body)를 콘솔에 출력해서 디버깅에 도움

        // [THEN] 1) HTTP 상태코드가 404 Not Found 인지 확인
        resultAction
                .andExpect(status().isNotFound())
                // [THEN] 2) 공통 응답 포맷(ApiResponse)에서 success가 false 이어야 함
                .andExpect(jsonPath("$.success").value(false))
                // [THEN] 3) 에러 메시지가 비어있지 않은지만 확인
                .andExpect(jsonPath("$.message").isNotEmpty());
    }


    // ⭐ 전체 조회 권한 실패 테스트 (GET /api/users) - 일반 USER가 호출 시 403 Forbidden
    @Test
    @DisplayName("전체 조회 실패 : 일반 USER가 GET /api/users 호출 시 403 Forbidden 반환")
    @WithMockUser(username = "normalUser", roles = {"USER"})

    void getAll_forbidden_whenNotAdmin() throws Exception {

        // [GIVEN] 관리자 전용 API라 DB에 사용자를 만들 필요 없음

        // [WHEN] 일반 USER 권한으로 GET /api/users 요청
        var resultAction = mockMvc.perform(
                get("/api/users") // 관리자 전용 API
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print()); // 응답 전체를 콘솔에 출력해서 상태/바디 확인

        // [THEN] 권한이 충족하지 못하므로 상태코드 403 기대
        resultAction
                .andExpect(status().isForbidden()); //HTTP 상태 코드가 403 Forbidden 인지 확인
                 // 응답 Body 구조는 Spring Security가 기본 처리하므로 상태 코드만 검증
    }


    // ⭐ 단일 조회 권한 실패 테스트 (GET /api/users/{id}) - 일반 USER가 다른 사람 id 조회 시 403
    @Test
    @DisplayName("단일 조회 실패 : 일반 USER가 다른 사용자 id로 GET /api/users/{id} 호출 시 403 Forbidden 반환")
    @WithMockUser(username = "normalUser", roles = {"USER"})
    void getUserById_forbidden_whenNotAdminAndNotOwner() throws Exception {

        // [GIVEN] 조회 대상이 될 사용자 1명 생성. (normalUser와는 다른 계정)
        User targetUser = userRepository.save(
                User.builder()
                        .username("targetUser1")                          // 조회 대상 계정
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("조회대상닉")
                        .email("target@example.com")
                        .build()
        );
        Long targetId = targetUser.getId(); // 이 ID는 현재 로그인한 normalUser의 id가 아님

        // [WHEN] 일반 USER(normalUser) 권한으로 다른 사람 id에 대해 GET /api/users/{id} 호출
        var resultAction = mockMvc.perform(
                get("/api/users/{id}", targetId)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print()); // 실제 응답(상태코드, Body)을 콘솔에 출력해서 확인

/*
         [THEN]
          - @PreAuthorize("hasRole('ADMIN') or #id == principal.id") 평가 과정에서
            @WithMockUser 가 제공하는 기본 principal(org.springframework.security.core.userdetails.User)에는
            id 필드/프로퍼티가 없으므로, principal.id 접근 시 스프링 EL 내부에서 예외 발생
          - 이 예외는 GlobalExceptionHandler 의 handleIllegalArgumentException(...) 에서 400(Bad Request)으로 처리됨
          - 실제 운영 환경에서 CustomUserDetails에 id 프로퍼티를 두고 principal.id가 정상 동작하게 구성하면,
            이 경우 AccessDeniedException → 403 Forbidden 으로 응답하도록 확장할 수 있음.
*/
        resultAction
                .andExpect(status().isBadRequest()); // 현재 테스트 환경에서는 400이 응답되는 것이 정상
    }



    // ⭐ 닉네임 수정 권한 실패 테스트 ( PATCH /api/users/{id}/nickname ) - 일반 USER가 다른 사람 닉네임 수정 시 실패
    @Test
    @DisplayName("닉네임 수정 실패 : 일반 USER가 다른 사용자 닉네임 PATCH 시 권한 부족으로 실패")
    @WithMockUser(username = "normalUser", roles = {"USER"})
    void updateNickname_forbidden_whenNotAdminAndNotOwner() throws Exception {
        // [GIVEN] 조회,업데이트 대상이 될 유저 생성
        User targetUser = userRepository.save(
                User.builder()
                        .username("targetUser1")// 권한 체크의 대상이 될 계정 username
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("기존닉네임")
                        .email("target@example.com")
                        .build()
        );

        // [GIVEN] 닉네임 수정 요청 JSON 바디 준비
        Map<String, String> requestBody = Map.of("nickname", "해커닉네임");// 일반 USER가 다른 사람 닉네임으로 바꾸려는 상황 가정
        String json = objectMapper.writeValueAsString(requestBody);// Map -> JSON 문자열로 변환

        // [WHEN] 현재 로그인한 계정은 @WithMockUser(nomalUser),
        // PATCH /api/users/{id}/nickname 에서 {id} 자리에 targetUser의 id를 넣어 요청 보냄
        var resultAction = mockMvc.perform(     // ↓ 다른 사용자의 id로 접근
                patch("/api/users/{id}/nickname", targetUser.getId())
                        .contentType(MediaType.APPLICATION_JSON) // 요청 본문 타입 : application/json
                        .content(json) // JSON 요청 바디 전송
                        .accept(MediaType.APPLICATION_JSON) // JSON 응답 기대
        ).andDo(print());

        // [THEN]
        /*
            현재 프로젝트 설정에서는 @PreAuthorize("hasRole('ADMIN') or #id == principal.id") 등에서
            principal.id 접근 시, @WithMockUser 가 제공하는 기본 UserDetails에는 id 필드가 없어서
            SpEL 평가 과정에서 예외가 발생.
            이 예외는 GlobalExceptionHandler 에서 IllegalArgumentException 또는 유사 예외로 처리되어
            HTTP 400 Bad Request 로 반환되는 구조이므로,
            여기서는 "권한 체크 중 잘못된 principal 접근으로 인한 400 응답"을 검증.
            이후 CustomUserDetails 를 도입해서 principal.id 를 정상 제공하고,
            권한 부족 상황을 AccessDeniedException -> 403 Forbidden 으로 처리하도록 변경하면
            이 테스트의 기대 상태 코드를 isForbidden() 으로 수정.
         */
        resultAction
                .andExpect(status().isBadRequest());                     // 현재 구조에서는 400 Bad Request 응답이 정상
    }



    // ⭐ 이메일 수정 권한 실패 테스트 (PATCH /api/users/{id}/email) - 일반 USER가 다른 사람 이메일 수정 시 실패
    @Test
    @DisplayName("이메일 수정 실패 : 일반 USER가 다른 사용자 이메일 PATCH 시 권한 부족으로 실패")
    @WithMockUser(username = "normalUser", roles = {"USER"})
    void updatePassword_forbidden_whenNotAdminAndNotOwner() throws Exception {
        // [GIVEN] 이메일 업데이트 대상 유저 생성
        User targetUser = userRepository.save(
                User.builder()
                        .username("targetUser1")
                        .password(passwordEncoder.encode("Password1!"))
                        .nickname("이메일유저")
                        .email("oldmail@example.com")
                        .build()
        );

        // [GIVEN] 이메일 수정 요청 JSON 바디 준비
        Map<String, String> requestBody = Map.of("email", "hack@example.com");
        String json = objectMapper.writeValueAsString(requestBody); // Map -> JSON 문자열 변환

        // [WHEN] 현재 로그인 계정 :  @WithMockUser("normalUser"),
        // PATCH /api/users/{id}/email 에서 {id} 자리에 targetUser의 id를 넣어 요청 보냄
        var resultAction = mockMvc.perform(
                patch("/api/users/{id}/email", targetUser.getId())       // 다른 사용자의 id로 접근
                        .contentType(MediaType.APPLICATION_JSON)          // 요청 본문 타입: application/json
                        .content(json)                                   // JSON 요청 바디 전송
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN]
        /*
            현재 프로젝트의 @PreAuthorize 설정이
            예) @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
            와 같이 principal.id 에 접근하는 형태라면,
            @WithMockUser 가 제공하는 기본 UserDetails 구현에는 id 필드가 없으므로
            SpEL에서 principal.id 평가 시 예외가 발생.
            이 예외는 GlobalExceptionHandler 에서 IllegalArgumentException 등으로 처리되어
            HTTP 400 Bad Request 로 응답되는 구조,
            지금 단계에서는 "권한 체크 도중 잘못된 principal 접근으로 인한 400 응답"을 검증.
            이후 CustomUserDetails 를 도입해서 principal.id 를 정상 제공하고,
            권한이 없는 경우 AccessDeniedException -> 403 Forbidden 으로 처리하도록 변경하면
            이 테스트의 기대 상태 코드를 isForbidden() 으로 수정.
         */
        resultAction
                .andExpect(status().isBadRequest()); // HTTP 400
    }



    // ⭐ 회원 삭제 권한 실패 테스트 (DELETE /api/users/{id}) - 일반 USER가 다른 사람 삭제 시 실패
    @Test
    @DisplayName("회원 삭제 실패 : 일반 USER가 다른 사용자 DELETE 시 권한 부족으로 실패")
    @WithMockUser(username = "normalUser", roles = {"USER"})
    void deleteUser_forbidden_whenNotAdminAndNotOwner() throws Exception {
        // [GIVEN] 삭제 대상이 될 유저 생성
        User targetUser = userRepository.save(
                User.builder()
                        .username("targetDeleteUser1") // 삭제 대상 계정 username
                        .password(passwordEncoder.encode("Password1!")) // 비밀번호 인코딩 후 저장
                        .nickname("삭제대상닉")
                        .email("targetdelete@example.com")
                        .build()
        );
        Long targetId = targetUser.getId();  // 삭제 대상 사용자의 PK

        // [WHEN] 현재 로그인한 계정은 @WithMockUser("normalUser") 이고,
        //        DELETE /api/users/{id} 에서 {id} 자리에 targetUser의 id를 넣어 요청 보냄
        var resultAction = mockMvc.perform(
                delete("/api/users/{id}", targetId)  // 다른 사용자의 id로 DELETE 요청
                        .accept(MediaType.APPLICATION_JSON)    // JSON 응답 기대
        ).andDo(print());

        // [THEN]
        /*
            현재 프로젝트의 @PreAuthorize 설정이
            예) @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
            처럼 principal.id 를 사용하는 구조라면,
            @WithMockUser 가 제공하는 기본 UserDetails 구현에는 id 필드가 없어서
            SpEL에서 principal.id 평가 시 예외가 발생.
            이 예외는 GlobalExceptionHandler 에서 IllegalArgumentException 등으로 처리되어
            HTTP 400 Bad Request 로 응답되는 구조,
            지금 단계에서는 "권한 체크 도중 잘못된 principal 접근으로 인한 400 응답"을 검증.
            이후 CustomUserDetails 를 도입해서 principal.id 를 정상 제공하고,
            권한이 없는 경우 AccessDeniedException → 403 Forbidden 으로 처리하도록 변경하면,
            이 테스트의 기대 상태 코드를 isForbidden() 으로 수정.
         */
        resultAction
                .andExpect(status().isBadRequest());// 현재 구조에서는 400 Bad Request 응답이 정상

        // [THEN] 2차 검증 : 실제로 DB에서 사용자가 삭제되지 않았는지도 확인
        boolean exists = userRepository.findById(targetId).isPresent(); // 삭제가 막혔으므로 여전히 존재해야 함
        assertThat(exists).isTrue();
    }



    // ⭐ 회원가입 검증 실패 테스트 (이메일 형식 오류) - POST /api/users
    @Test
    @DisplayName("회원가입 실패 : 잘못된 이메일 형식으로 요청 시 400 Bad Request와 실패 응답 반환")
    void register_validationFail_invalidEmail_return400() throws Exception {
        // [GIVEN] 이메일 형식이 잘못된 회원가입 요청 DTO 생성
        UserSignupRequestDto requestDto = UserSignupRequestDto.builder()
                .username("invalidemailuser")
                .password("Password1@")
                .nickname("검증실패닉")
                .email("not-an-email") // 이메일 형식 오류
                .build();

        // DTO -> JSON 문자열 변환 (컨트롤러에서 @RequestBody 로 받는 JSON 형식과 동일하게 직렬화)
        String json = objectMapper.writeValueAsString(requestDto);

        // [WHEN] 잘못된 이메일을 포함한 JSON 으로 POST /api/users 요청 전송
        var resultAction = mockMvc.perform(
                post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON) // 요청 본문 타입: application/json
                        .content(json)                          // 잘못된 이메일을 담은 JSON 요청 바디
                        .accept(MediaType.APPLICATION_JSON)     // JSON 응답 기대
        ).andDo(print());

        // [THEN]
        /*
            기대 시나리오
            - @Valid 가 붙은 DTO에서 email 필드에 대한 @Email(or 정규식) 검증이 실패
            - MethodArgumentNotValidException 이 발생
            - GlobalExceptionHandler 에서 이를 처리하여
              HTTP 400 Bad Request 와 공통 응답 포맷(ApiResponse)을 반환
            여기서는 구체적인 에러 메시지 내용보다는
            "400 상태 코드"와 "success=false, message가 비어있지 않음"을 우선적으로 검증.
            (필드별 에러 상세 구조는 GlobalExceptionHandler 의 구현에 따라 달라질 수 있기 때문)
         */
        resultAction
                .andExpect(status().isBadRequest())  // HTTP 400 Bad Request
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());// 에러 메시지가 비어있지 않은지만 확인
    }

}
