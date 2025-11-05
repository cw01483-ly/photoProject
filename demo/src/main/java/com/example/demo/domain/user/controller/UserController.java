package com.example.demo.domain.user.controller;


import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
    - 현재는 DTO가 없으므로, 임시로 엔티티(User)를 요청/응답 바디로 사용.
      추후 DTO(UserSignupRequestDto, UserLoginRequestDto, UserResponseDto) 도입 시 메서드 시그니처를 교체.
    - 예외(예: EntityNotFoundException, IllegalStateException)는 전역 예외 처리기(@ControllerAdvice) 추가 시 한글 표준 응답으로 변환 예정.
 */
    private final UserService userService; //서비스 계층 의존성(비지니스 로직 호출하기)
    // HTTP POST요청 처리, 사용자 등록
    @PostMapping
    public ResponseEntity<User> register(@Valid @RequestBody User request){
        //Valid : 유효성검사
        /*ResponseEntity : 스프링MCV의 응답용 클래스(SpringFramework)
            서버가 클라이언트에게 보낼 HTTP응답(상태,본문,헤더)을 직접 제어함
        */
        //@RequestBody User request : 사용자가 회원가입폼에서 입력한 정보가 JSON형태로 서버에 오면 User객체로 자동변경
        /*ResponseEntity<User> : HTTP 상태코드/헤더/바디 를 직접 구현 후 반환. 바디타입은 User
        * 서버가 보내는 응답을 직접 구성하는 객체(어떤 상태로 어떤 내용을 보낼지 직접 설정)*/
        User saved = userService.register(request); //회원등록 (비즈니스 로직 실행)

        URI location = URI.create("/api/users/"+saved.getId());
        //새로 등록된 사용자의 PK를 꺼내서 고유주소(URI : Uniform Resource Identifier)를 만들기
        return ResponseEntity.created(location).body(saved);
        // 201 Created 라는 상태코드 자동으로 붙이기
    }


    //READ 단일, 전체, username 조회 (사용자 조회)

    //users PK값으로 조회
    @GetMapping("/{id}") //Get요청 /api/users/{id} 요청 처리
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") //2차방어선
    public ResponseEntity<User> getById(@PathVariable Long id){//@PathVariable Long id :URL의{id}를 변수에 담음
        User user = userService.getById(id); //실행 비즈니스 로직, 유저서비스호출해서 명령전달
        return ResponseEntity.ok(user); // HTTP코드 200 응답과 함께 User정보 반환
    }

    //전체조회 (관리자용)
    @GetMapping // GET /api/users
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAll(){
        List<User> users = userService.getAll();
        return ResponseEntity.ok(users);
    }

    //username으로 조회 (관리자용)
    @GetMapping("/username/{username}") // GET /api/users/username/{username}
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getByUsername(@PathVariable String username){
        User user = userService.getByUsername(username);
        return ResponseEntity.ok(user); // 200 OK
    }

    // UPDATE 닉네임, 이메일 부분 수정하기 (본인 or 관리자)

    //닉네임 수정하기
    @PatchMapping("/{id}/nickname") // PATCH /api/users/{id}/nickname
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<User> updateNickname(
            @PathVariable Long id,
            @RequestBody NicknameUpdateRequest request // 간단 요청 바디(임시 DTO)
    ){
        User updated = userService.updateNickname(id, request.getNickname());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/email") // PATCH /api/users/{id}/email
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") // 본인 또는 관리자만
    public ResponseEntity<User> updateEmail(
            @PathVariable Long id,
            @RequestBody EmailUpdateRequest request // 간단 요청 바디(임시 DTO)
    ){
        User updated = userService.updateEmail(id, request.getEmail());
        return ResponseEntity.ok(updated);
    }

    // Delete: 회원 삭제(본인 or 관리자)/

    @DeleteMapping("/{id}") // DELETE /api/users/{id}
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") // 본인 또는 관리자만
    public ResponseEntity<Void> delete(@PathVariable Long id){
        userService.delete(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    //(임시) 요청 바디 모델: 컨트롤러 내부 static 클래스들
    // 실제 운영에서는 별도 DTO 파일로 분리하고 @Valid, @NotBlank, @Email 등을 부여

    public static class NicknameUpdateRequest {
        private String nickname; // JSON: { "nickname": "새닉네임" }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    public static class EmailUpdateRequest {
        private String email; // JSON: { "email": "new@example.com" }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
