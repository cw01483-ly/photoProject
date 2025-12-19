package com.example.demo.domain.user.service;

import com.example.demo.domain.user.dto.UserLoginRequestDto;
import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.exception.AuthenticationFailException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
// 테스트 이름(한글 설명)을 붙일 때 사용하는 어노테이션
import org.junit.jupiter.api.Test;
// "이 메서드는 테스트입니다" 표시하는 JUnit5 어노테이션

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
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
    // ⭐ 회원가입 테스트
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

    // ⭐ Username 중복시 실패 테스트
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

    // ⭐ Email 중복 실패 테스트
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

    // ⭐ 로그인 성공 테스트
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

    // ⭐ 로그인 실패 테스트 ( 비밀번호 불일치 )
    @Test
    @DisplayName("로그인 실패 : 비밀번호 틀리면 예외 발생")
    void login_fail_wrongPw(){
        // [GIVEN] 1) 정상 회원 생성
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username("loginUser1")
                .password("CorrectPassword1!")
                .nickname("로그인실패유저")
                .email("login1@example.com")
                .build();

        userService.register(signupRequest); // 회원가입 -> DB저장 +Pw암호화

        // 2) 로그인 요청 DTO를 "틀린 Pw"로 만들기
        UserLoginRequestDto wrongPwLoginRequest = UserLoginRequestDto.builder()
                .username("loginUser1")
                .password("WorngPassword1!")
                .build(); // DTO 생성 완료

        // [WHEN & THEN], 틀린 비밀번호 로그인 시도 시 예외 발생
        assertThrows( //assertThrows : 특정 코드 실행 시 [예외 발생해야 한다]는 것을 검증
                AuthenticationFailException.class, // 예외타입
                        ()-> userService.login(wrongPwLoginRequest)); // 실제 실행할 코드
    }

    // ⭐ 로그인 실패 테스트 ( username이 존재하지 않을 때 )
    @Test
    @DisplayName("로그인 실패 : username 없으면 예외 발생")
    void login_fail_userNotFound(){
        // [GIVEN] 존재하지 않는 username 사용하여 로그인 요청 DTO 생성
        UserLoginRequestDto request = UserLoginRequestDto.builder()
                .username("no_such_user1")// DB에 존재하지 않는 username 사용
                .password("SomePassword1!")// 비밀번호는 의미 없음
                .build();
        /* [WHEN & THEN]
            userService.login(request)를 실행할 때
            AuthenticationFailException 이 발생하면 테스트 성공
        */
        assertThrows(
                AuthenticationFailException.class,
                () -> userService.login(request));
    }

    // ⭐ 사용자 조회 테스트
    @Test
    @DisplayName("사용자 조회 성공 : 존재하는 ID로 조회 시 User 반환")
    @WithMockUser(username = "finduser1", roles = {"ADMIN"}) // 가상의 인증된 사용자 정보 주입
    void getById_success(){
        // [GIVEN] 1) 테스트용 유저 1명 생성
        UserSignupRequestDto request = UserSignupRequestDto.builder()
                .username("findUser1")
                .password("Password123!")
                .nickname("닉네임설정")
                .email("example@example.com")
                .build();

        // userService.register() 호출 -> User 엔티티 DB 저장
        User saved = userService.register(request);

        // saved.getId() 는 DB PK값 -> 실제 findById 테스트에 사용할 ID
        Long saveId = saved.getId();

        // [WHEN] 2) getById 호출
        User found = userService.getById(saveId);

        // [THEN] 3) 반환된 User 엔티티 검증
        assertThat(found).isNotNull();// 3-1) 존재 해야함
        assertThat(found.getId()).isEqualTo(saveId); // 3-2) 조회된 PK 동일한지 확인
        assertThat(found.getUsername()).isEqualTo("finduser1");
        // ↑ 3-3) username 그대로 나오는지 확인, trim() 직접 설정
        assertThat(found.getEmail()).isEqualTo("example@example.com"); // 3-4) 메일 일치 확인
        assertThat(found.getNickname()).isEqualTo("닉네임설정"); // 3-5 닉네임 일치 확인
    }

    // ⭐ 사용자 조회 실패 테스트
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @Test
    @DisplayName("사용자 조회 실패 : 존재하지 않는 ID 조회 시 예외 발생")
    void getById_fail_userNotFound(){

        //[GIVEN] 존재하지 않는 사용자 ID 준비
        Long invalidId = 999999L; // 실제 DB에 존재 할 가능성 거의 없는 큰 값 사용

        // [WHEN & THEN] getById(invalidId) 실행 시 EntityFoundException 발생
        assertThrows(
                EntityNotFoundException.class, //예상되는 예외 타입
                () -> userService.getById(invalidId)); // 실행할 코드
    }

    // ⭐ 닉네임 업데이트 성공 테스트
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    // @PreAuthorize("hasRole('ADMIN') or #id == principal.id") 통과용 가짜 인증 사용자 (ADMIN 권한 부여)
    @Test
    @DisplayName("닉네임 업데이트 성공 : 유효한 새 닉네임으로 수정 시 반영")
    void updateNickname_success(){
        // [GIVEN] 초기 닉네임 유저 생성
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username("nickUser1")
                .password("Password123!")
                .nickname("기본닉네임")
                .email("example@example.com")
                .build();

        User saved = userService.register(signupRequest); // DB에 User엔티티 저장

        Long userId = saved.getId(); // 업데이트에 사용할 PK

        // [WHEN] 2) 닉네임 업데이트 서비스 호출
        String newNickname = "새로운닉네임";
        User updated = userService.updateNickname(userId,newNickname);
        /* updateNickname 내부에서 getById() 로 유저조회, 중복검사,
            user.changeNickname() 호출하여 엔티티 상태 변경*/

        // [THEN] 3) 반환된 User와 DB 상태 검증
        assertThat(updated).isNotNull(); // 3-1) null 이 아니어야 함
        assertThat(updated.getId()).isEqualTo(userId); // 3-2) 같은 사용자여야 함
        // 3-3) 닉네임이 새 값으로 변경되었는지 확인
        assertThat(updated.getNickname()).isEqualTo(newNickname);

        // DB에서 다시 조회 ( 재확인, 신뢰도 향상 )
        User found = userService.getById(userId);
        assertThat(found.getNickname()).isEqualTo(newNickname);
    }


    // ⭐ 닉네임 수정 실패 테스트 (중복 닉네임)
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    // ADMIN 권한이므로 @PreAuthorize 조건을 통과할 수 있게 설정
    @Test
    @DisplayName("닉네임 수정 실패 : 이미 사용 중인 닉네임으로 변경 시 예외 발생")
    void updateNickname_fail_duplicateNickname() {
        // [GIVEN] 1) 서로 다른 닉네임을 가진 유저 2명 생성
        UserSignupRequestDto user1Request = UserSignupRequestDto.builder()
                .username("dupNickUser1")
                .password("Password123!")
                .nickname("닉네임1")
                .email("dup1@example.com")
                .build();

        UserSignupRequestDto user2Request = UserSignupRequestDto.builder()
                .username("dupNickUser2")
                .password("Password123!")
                .nickname("닉네임2")
                .email("dup2@example.com")
                .build();

        User user1 = userService.register(user1Request); // 닉네임 "닉네임1"
        User user2 = userService.register(user2Request); // 닉네임 "닉네임2"

        Long targetId = user2.getId(); // 닉네임을 바꿀 대상 유저 (현재 닉네임2 보유)

        /* [WHEN & THEN]
         user2의 닉네임을 user1의 닉네임("닉네임1")으로 바꾸려고 하면
         UserService.updateNickname 내부에서
          - findByNickname("닉네임1") 으로 다른 사용자 발견
          - conflict.getId() != id 조건을 만족하여 IllegalStateException 을 던져야 함.*/
        assertThrows(
                IllegalStateException.class,  // 예상 예외 타입
                () -> userService.updateNickname(targetId, "닉네임1") // 실행할 코드 (람다식)
        );
    }


    // ⭐ 이메일 업데이트 성공 테스트
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @Test
    @DisplayName("이메일 업테이트 성공 : 유효한 새 이메일 수정 시 반영")
    void updateEmail_success(){
        // [GIVEN] 1) 초기 사용자 생성
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username("emailUser1")
                .password("Password123!")
                .nickname("이메일유저")
                .email("before@example.com")
                .build();

        User saved = userService.register(signupRequest); // DB에 User 엔티티 저장
        Long userId = saved.getId(); // 이메일업데이트 에 사용할 PK

        // [WHEN] 2) 이메일 업데이트 서비스 호출
        String newEmail = "new@example.com"; // 변경할 이메일
        User updated = userService.updateEmail(userId, newEmail);

        // [THEN] 3) 변환된 User와 DB 상태 검증
        assertThat(updated).isNotNull(); // 3-1) null X
        assertThat(updated.getId()).isEqualTo(userId); // 3-2) 같은 사용자인지
        assertThat(updated.getEmail()).isEqualTo(newEmail); // 3-3) 이메일이 새 값으로 변경되었는지 확인

        // DB에서 다시 조회, 반영 여부 재확인
        User found = userService.getById(userId);
        assertThat(found.getEmail()).isEqualTo(newEmail);
    }


    // ⭐ 이메일 업데이트 실패 테스트 (중복 이메일)
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    @Test
    @DisplayName("이메일 업데이트 실패 : 사용중인 이메일로 변경 시 예외 발생")
    void updateEmail_fail_duplicateEmail(){
        // [GIVEN] 1) 서로 다른 이메일 가진 사용자 2명 생성
        UserSignupRequestDto user1Request = UserSignupRequestDto.builder()
                .username("dupEmailUser1")
                .password("Password123!")
                .nickname("이메일닉1")
                .email("dupEmail1@example.com")
                .build();

        UserSignupRequestDto user2Request = UserSignupRequestDto.builder()
                .username("dupEmailUser2")
                .password("Password123!")
                .nickname("이메일닉2")
                .email("dupEmail2@example.com")
                .build();

        User user1 = userService.register(user1Request);
        User user2 = userService.register(user2Request);

        Long targetId = user2.getId(); // 이메일 바꿀 사용자

        /* [WHEN & THEN]
            user2의 이메일을 user1의 이메일로 변경하려 하면 UserService.updateEmail 내부에서
                findByEmail() 으로 다른 사용자 발견
                    -> conflict.getId() != id 조건을 만족, 예외 발생
        */
        assertThrows(
                IllegalStateException.class, //예상 예외 타입
                () -> userService.updateEmail(targetId, "dupEmail1@example.com")// 실행 할 코드
        );
    }


    // ⭐ 존재하지 않는 사용자 삭제 실패 테스트
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @Test
    @DisplayName("회원 삭제 실패 : 존재하지 않는 ID 삭제 시 예외 발생")
    void deleteUser_fail_userNotFound(){
        // [GIVEN] 실제 DB에 존재하지 않는 사용자 ID 준비
        Long invalidId = 99999999L;

        /* [WHEN & THEN]
            존재하지 않는 ID로 userService.delete(invalidId)를 호출시
            내부에서 getById(invalidId)실행 >> EntitiyNotFoundException 발생
        */
        assertThrows(
                EntityNotFoundException.class, // 예상 예외 타입
                () -> userService.delete(invalidId)); // 실제 실행 코드
    }


    // ⭐ 회원 삭제 성공 테스트
    @WithMockUser(username = "adminuser", roles = {"ADMIN"})
    // ADMIN 권한의 가짜 사용자로 인증 -> 삭제 권한 보장
    @Test
    @DisplayName("회원 삭제 성공 : 존재하는 ID 삭제 시 DB에서 제거")
    void deleteUser_success() {
        // [GIVEN] 1) 테스트용 사용자 한 명 생성 및 저장
        UserSignupRequestDto signupRequest = UserSignupRequestDto.builder()
                .username("deleteUser1")          // 삭제 대상 username
                .password("Password123!")         // 임의 비밀번호
                .nickname("삭제대상유저")          // 닉네임
                .email("delete1@example.com")     // 이메일
                .build();

        User saved = userService.register(signupRequest); // 회원가입 -> DB에 User 엔티티 저장
        Long userId = saved.getId(); // 이후 삭제에 사용할 PK 값

        // [WHEN] 2) userService.delete 호출로 회원 삭제 시도
        userService.delete(userId);

        // [THEN] 3) userRepository로 다시 조회했을 때 존재하지 않아야 함
        boolean exists = userRepository.findById(userId).isPresent(); // Optional이 비어 있는지 여부 확인
        assertThat(exists).isFalse(); // 삭제 후에는 반드시 false 여야 함 (DB에서 제거됨)
    }
}
