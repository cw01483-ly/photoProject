package com.example.demo.domain.user.controller;


import com.example.demo.domain.user.dto.UserLoginRequestDto;
import com.example.demo.domain.user.dto.UserResponseDto;
import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserService;
import com.example.demo.global.response.ApiResponse;
import com.example.demo.global.security.CustomUserDetails;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import com.example.demo.global.security.jwt.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController // JSON 기반의 REST(Representational State Transfer) 응답을 반환하는 컨트롤러
/*
    RestController 는 @Controller + @ResponseBody
    @Controller : 해당 클래스가 웹클라이언트(브라우저)의 요청을 처리하는 컨트롤러임을 스프링에게 알림
    @ResponseBody : 메서드가 반환하는 값이 View(화면)가 아니라 HTTP응답 본문(Body)으로 직접 전송되는 데이터임을 명시
    >> 해당 클래스를 HTTP요청을 받고, 데이터를 JSON형태로 반환하는 REST API의 엔드포인트로 설정하겠다.
 */
@RequestMapping("/api/users") //이 컨트롤러의 공통 URL prefix(접두사)
/*해당 클래스로 들어온 모든 메서드가 지정된 경로로 사용하게 됨
    >> /api/users로 시작하는 모든 요청을 처리할 준비 */
@RequiredArgsConstructor //생성자 주입 자동생성 (final 필드 : userService)
@Validated //매서드 파라미터/바디에 대한 Bean Validation 활성화
public class UserController {
    // Get : 데이터 조회 | Post : 등록 | Put : 수정 | Delete : 삭제

    /*
    UserController
    - UserService를 호출하여 사용자 관련 HTTP 요청을 처리하는 REST 컨트롤러
    - 현재 컨트롤러는 DTO(UserSignupRequestDto, UserResponseDto) 기반으로 요청/응답을 처리.
      (UserLoginRequestDto는 추후 로그인 API에서 사용 예정)
    - 예외(예: EntityNotFoundException, IllegalStateException)는
        전역 예외 처리기(@ControllerAdvice) 추가 시 한글 표준 응답으로 변환 예정.
 */
    private final UserService userService; //서비스 계층 의존성(비지니스 로직 호출하기)
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    // HTTP POST요청 처리, 사용자 등록
    @PostMapping //HTTP POST요청 처리, 사용자 등록
    public ResponseEntity<ApiResponse<UserResponseDto>> register(@Valid @RequestBody UserSignupRequestDto request){
        //Valid : 유효성검사
        /*ResponseEntity : 스프링MVC의 응답용 클래스(SpringFramework)
            서버가 클라이언트에게 보낼 HTTP응답(상태,본문,헤더)을 직접 제어함
        */
        //@RequestBody UserSignupRequestDto request : 회원가입 요청 DTO로 받음(입력 검증/보안)
        /*ResponseEntity : HTTP 상태코드/헤더/바디 를 직접 구현 후 반환.
        서버가 보내는 응답을 직접 구성하는 객체(어떤 상태로 어떤 내용을 보낼지 직접 설정)*/
        User saved = userService.register(request); //회원등록 (비즈니스 로직 실행)

        URI location = URI.create("/api/users/"+saved.getId());
        //새로 등록된 사용자의 PK를 꺼내서 고유주소(URI : Uniform Resource Identifier)를 만들기
        return ResponseEntity.created(location)
                .body(ApiResponse.success(UserResponseDto.from(saved), "회원가입 성공"));
        // 201 Created + Location 헤더 + 응답 바디(민감 정보를 제거한 UserResponseDto) 반환
    }

    // 로그인 처리
    @PostMapping("/login") // Post/api/users/Login
    public ResponseEntity<ApiResponse<UserResponseDto>> login(
            @Valid @RequestBody UserLoginRequestDto request //username,password 평문입력
            ){
        /*
            - 로그인 요청을 처리하는 엔드포인트
            - UserService에 위임하여 아이디,비밀번호 검증 및 마지막 로그인 시각 갱신 수행
            - 성공 시 JWT를 HttpOnly 쿠키로 발급 ( 브라우저 자동전송, JS접근 불가 >> XSS 방어 )
            - 응답 바디엔 UserResponseDto만 포함 (토큰 노출 X)
        */
        // 1) 로그인 처리 (AuthenticationManager -> SecurityContext 저장까지 완료)
        UserResponseDto response = userService.login(request);

        /*
            JWT 발급을 위해 현재 인증된 사용자 정보(principal) 꺼내기
            - UserService.login() 에서 SecurityContextHolder에 Authentication이 이미 저장,
               principal 타입은 CustomUserDetails
         */
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails principal =
                (CustomUserDetails) authentication.getPrincipal();

        /*
            JWT Access Token 생성
             - subject : username
             - claims  : id, role
             - 만료시간  : jwt, accessTokenExpMinutes 설정값 기준
         */
        String accessToken = jwtService.generateAccessToken(principal);

        /*
             HttpOnly 쿠키 생성
        - HttpOnly : JS 접근 차단 (XSS 방어)
        - Path=/   : 모든 API 요청에 쿠키 포함
        - Max-Age  : JWT 만료 시간과 동일하게 설정
        - Secure   : HTTPS 환경에서만 true 권장
     */
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getCookieName(), accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(jwtProperties.getAccessTokenExpMinutes()*60L)
                // .secure(true) HTTPS 적용 시 활성화
                .build();

        // 2) 응답 헤더에 Set-Cookie 추가 + 공통 응답 포멧 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response, "로그인 성공"));
    }

    //전체조회 (관리자용)
    @GetMapping // GET /api/users
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAll(){
        List<User> users = userService.getAll(); // 전체 사용자 엔티티 목록 조회
        List<UserResponseDto> result = users.stream()
                .map(UserResponseDto::from) //엔티티 -> DTO로 일괄 변환
                .collect(Collectors.toList()); // Java 8 호환 수집 (또는 .toList() 사용 가능)
        // 목록도 공통 포맷으로 포장
        return ResponseEntity.ok(ApiResponse.success(result, "전체 사용자 조회 성공"));
    }

    //username으로 조회 (관리자용)
    @GetMapping("/username/{username}") // GET /api/users/username/{username}
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> getByUsername(@PathVariable String username){
        User user = userService.getByUsername(username); // username 기준 단일 조회
        return ResponseEntity.ok
                (ApiResponse.success(UserResponseDto.from(user), "username 기준 조회 성공")); // 200 OK + 단일DTO 반환
    }

    // UPDATE 닉네임, 이메일 부분 수정하기 (본인 or 관리자)

    //닉네임 수정하기
    @PatchMapping("/{id}/nickname") // PATCH /api/users/{id}/nickname
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateNickname(
            @PathVariable Long id,
            @Valid @RequestBody NicknameUpdateRequest request
            // 간단 요청 바디(임시 DTO) +@Valid로 공백 검증
    ){
        User updated = userService.updateNickname(id, request.getNickname());
        return ResponseEntity.ok
                (ApiResponse.success
                        (UserResponseDto.from(updated), "닉네임 수정 성공"));
        //변경 결과를 응답 DTO로 반환
    }

    //이메일 수정
    @PatchMapping("/{id}/email") // PATCH /api/users/{id}/email
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") // 본인 또는 관리자만
    public ResponseEntity<ApiResponse<UserResponseDto>> updateEmail(
            @PathVariable Long id,
            @Valid @RequestBody EmailUpdateRequest request // 간단 요청 바디(임시 DTO) + 공백,형식검사
    ){
        User updated = userService.updateEmail(id, request.getEmail());//서비스에서 변경/검증
        return ResponseEntity.ok
                (ApiResponse.success(UserResponseDto.from(updated)
                        , "이메일 수정 성공"));//DTO로 감싸 반환
    }

    // Delete: 회원 삭제(본인 or 관리자)/

    @DeleteMapping("/{id}") // DELETE /api/users/{id}
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") // 본인 또는 관리자만
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id){
        userService.delete(id);
        // 일관성을 위해 200 OK + 메시지로 응답 (기존 204 No Content 대신)
        return ResponseEntity.ok(ApiResponse.success("회원 삭제 성공"));
        // 만약 204를 유지하고 싶다면: return ResponseEntity.noContent().build();
    }

    //(임시) 요청 바디 모델: 컨트롤러 내부 static 클래스들
    // 실제 운영에서는 별도 DTO 파일로 분리하고 @Valid, @NotBlank, @Email 등을 부여

    public static class NicknameUpdateRequest {
       @NotBlank(message = "닉네임은 공백일 수 없습니다.") // 컨트롤러 입구 검증
        private String nickname; // JSON : { "nickname" : "새닉네임"}

        public String getNickname(){return nickname;} //직렬화,검증 을 위한 getter
        public void setNickname(String nickname){this.nickname=nickname;}//역직렬화 setter
        }

    public static class EmailUpdateRequest {
        @NotBlank(message = "이메일은 필수 입력 사항입니다.")
        @Email(message = "올바른 이메일 형식을 입력하세요")
        private String email; // JSON: { "email": "new@example.com" }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
