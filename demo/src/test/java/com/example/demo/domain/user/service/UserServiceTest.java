package com.example.demo.domain.user.service;

import com.example.demo.domain.user.dto.UserLoginRequestDto;
import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
// 테스트 이름(한글 설명)을 붙일 때 사용하는 어노테이션
import org.junit.jupiter.api.Test;
// "이 메서드는 테스트입니다" 표시하는 JUnit5 어노테이션

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
// AssertJ의 assertThat 사용 (가독성 좋은 검증 코드 작성용)

/*
    UserServiceTest
    - UserService.register(회원가입) 기능이 정상 동작하는지 검증하는 테스트 클래스
    - 스프링 컨텍스트를 실제로 띄워서, 진짜 UserService & UserRepository 빈을 사용함
    - 각 테스트는 @Transactional 덕분에 실행 후 DB에 남지 않음 (테스트 데이터 자동 롤백)
 */
@SpringBootTest // 스프링 부트 전체를 띄운 상태에서 테스트한다는 의미
@Transactional  // 각 테스트 메서드가 끝날 때마다 롤백 처리 (DB 깨끗하게 유지)
public class UserServiceTest {

    @Autowired
    private UserService userService;
    // 실제 어플에서 사용 중인 UserService 빈을 주입받고 register 메서드를 직접 호출, 테스트

    @Autowired
    private UserRepository userRepository;
    // User가 실제로 DB에 잘 저장되었는지 확인할 때 사용
    // saved.getId()로 바로 확인해도 되지만, DB에서 다시 조회해보면 더 확실함

    @Test
    @DisplayName("회원가입 성공 : 올바른 값이면 User가 저장되고, 비밀번호는 암호화")
        // 테스트 실행 창에서 한글로 보이게
    // 회원가입 테스트
    void register_success(){
        // [GIVEN] 테스트에 사용할 입력값 준비
        UserSignupRequestDto request = UserSignupRequestDto.builder()
                .username("testuser1")
                .password("Password123!")
                .nickname("test닉네임")
                .email("test@example.com")
                .build();

        // [WHEN] 실제 회원가입 로직 실행 -> User 엔티티 반환
        User saved = userService.register(request);

        // [THEN] 결과 검증
        // 1) DB에서 다시 조회
        User found = userRepository.findById(saved.getId())
                .orElseThrow(()-> new IllegalStateException("저장된 유저를 찾지 못했습니다."));

        // 2) PK가 존재 해야함 -> DB 저장 성공 증거
        assertThat(found.getId()).isNotNull();

        // 3) 입력한 값이 그대로 저장 되었는지 검증
        assertThat(found.getUsername()).isEqualTo("testuser1");
        assertThat(found.getEmail()).isEqualTo("test@example.com");
        assertThat(found.getNickname()).isEqualTo("test닉네임");

        // 4) 비밀번호 암호화 확인 (비밀번호가 평문으로 저장되면 안됨)
        assertThat(found.getPassword()).isNotEqualTo("Password123!");
        assertThat(found.getPassword()).isNotBlank();// 암호화된 비밀번호가 존재해야 함
    }

    // Username 중복시 실패 테스트
    @Test
    @DisplayName("회원가입 실패 : username 중복되면 예외 발생")
    void register_fail_usernameDuplicated(){
        //[GIVEN]
        UserSignupRequestDto req1 = UserSignupRequestDto.builder()
                .username("duplicateUser1")
                .password("Password123!")
                .nickname("닉네임1")
                .email("email1@example.com")
                .build();

        UserSignupRequestDto req2 = UserSignupRequestDto.builder()
                .username("duplicateUser1")
                .password("Password123!")
                .nickname("닉네임2")
                .email("email2@example.com")
                .build();

        //[WHEN]
        userService.register(req1);

        //[THEN]
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(req2)
        );
    }

    // Email 중복 실패 테스트
    @Test
    @DisplayName("회원가입 실패 : email 중복 시 예외 발생")
    void register_fail_emailDuplicated(){
        //[GIVEN]
        UserSignupRequestDto req1 = UserSignupRequestDto.builder()
                .username("testuserA1")
                .password("Password123!")
                .nickname("test닉네임A")
                .email("same@example.com")
                .build();

        UserSignupRequestDto req2 = UserSignupRequestDto.builder()
                .username("testuserB1")
                .password("Password123!")
                .nickname("test닉네임B")
                .email("same@example.com") // email 중복
                .build();

        //[WHEN] 첫번째 정상 가입
        userService.register(req1);

        //[THEN] 두번째 가입은 예외 발생
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(req2)
        );
        /* assertThrows(예상되는예외클래스, 실행할코드);
            첫 번째 인자 : "어떤 예외가 발생해야 테스트가 성공하는지"
                → 예: IllegalArgumentException.class
            두 번째 인자 : "예외가 발생해야 하는 실제 실행 코드"
                → () -> userService.register(req2)
                 (람다식으로 감싸야 한다. 바로 호출하면 assertThrows가 감지할 수 없음)
            ==> register(req2) 실행했을 때 IllegalArgumentException 발생하면 성공
        */
    }

    // 로그인 성공 테스트
    @Test
    @DisplayName("로그인 성공 : 올바른 username,password로 로그인 시 UserResponseDto 반환")
    void login_success(){
        //[GIVEN] 1) 회원가입을 통해 유저하나 생성
        String rawUsername = "loginUser1"; // 사용자가 입력한 원본 (대소문자+숫자)
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username(rawUsername)
                .password("LoginPassword1")
                .nickname("로그인유저")
                .email("login@example.com")
                .build();

        userService.register(signupRequest); // 회원가입>>DB저장+비밀번호 암호화

        // 2) 로그인 요청 DTO (username + 평문password)
        UserLoginRequestDto loginRequest = UserLoginRequestDto.builder()
                .username(rawUsername)
                .password("LoginPassword1")
                .build();

        //[WHEN] 로그인 시도, UserResponseDto 반환 가정
        var response = userService.login(loginRequest);

        //[THEN] 로그인 결과 검증
        assertThat(response.getUsername()).isEqualTo(rawUsername.toLowerCase());

        // 실제 DB에서 다시 꺼내서 마지막 로그인 시간 갱신확인하려면
        User found = userRepository.findByUsername("loginUser1")
                .orElseThrow(() -> new IllegalStateException("로그인 유저를 찾지 못했습니다."));

        assertThat(found.getLastLoginAt()).isNotNull();
    }
}
