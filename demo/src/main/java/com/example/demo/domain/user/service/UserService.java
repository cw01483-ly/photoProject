package com.example.demo.domain.user.service;


import com.example.demo.domain.user.dto.UserLoginRequestDto;
import com.example.demo.domain.user.dto.UserResponseDto;
import com.example.demo.domain.user.dto.UserSignupRequestDto;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.exception.AuthenticationFailException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List; //목록 반환용

@Slf4j
@Service
@RequiredArgsConstructor // final필드(UserRepository)로 생성자를 자동으로 생성
@Transactional(readOnly = true) //기본적으로 읽기 전용 트랜잭션 ( 성능 최적화 )
public class UserService {
    private final UserRepository userRepository; // DB접근을 담당하는 Repository
    private final PasswordEncoder passwordEncoder; //비밀번호 암호화용 의존성
    private final AuthenticationManager authenticationManager; // Spring Security 인증 처리(세션/컨텍스트 저장에 사용)

    @Transactional //쓰기 작업이므로 readOnly=false 로 오버라이드
    public User register(@Valid UserSignupRequestDto dto){ // 회원 등록(Create)
        // 1) username 중복검사
        validateDuplicateUsername(dto.getUsername()); //validateDuplicate : 중복검증

        //  email 중복 검사 (null 이 아닐때만 체크하기)
        if(dto.getEmail() != null && !dto.getEmail().isBlank()){
            validateDuplicateEmail(dto.getEmail());
        }
        // 2) 비밀번호 암호화
        String encoded = passwordEncoder.encode(dto.getPassword());

        // 3) DTO -> 엔티티 변환 (엔티티 @Builder 사용)
        User entity = User.builder()
                .username(dto.getUsername())
                .password(encoded)
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .build();
        //엔티티의 @PrePersist 훅에서 username,email trim과 소문자변환 정규화 수행

        // 4) 저장, 검증 통과 시 Repository를 통해 저장하고, 영속화된 엔티티 반환
        return userRepository.save(entity);
        /*추 후 확장 할 것들
        *  - PasswordEncoder 적용
        *  - 회원가입 이벤트 발행(알림/이메일 인증)*/
    }

    //로그인 메서드
    @Transactional
    public UserResponseDto login(UserLoginRequestDto request){ //로그인 요정DTO를 받아 응답DTO를 반환하는 메서드 시작

        // 요청 객체 자체 null 방어
        if (request == null){
            throw new IllegalArgumentException("로그인 요청이 비어있습니다.");
        }
        // 1) 요청 DTO에서 아이디, 비밀번호 원본 문자열 꺼내기
        String rawUsername = request.getUsername(); //사용자가 입력한 username 가져오기
        String rawPassword = request.getPassword(); // 사용자가 입력한 password(암호화 전)

        // 2) 아이디, 비밀번호의 공백,null 기본 검증
        if (rawUsername == null || rawUsername.isBlank()){
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (rawPassword == null || rawPassword.isBlank()){
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // 3) 아이디 문자열 정규화 (비밀번호는 정규화 X)
        String normalizedUsername = rawUsername.trim().toLowerCase();

        /*
            ★ 변경 핵심: AuthenticationManager 기반 로그인
            - SecurityConfig에서 AuthenticationManager가 CustomUserDetailsService + PasswordEncoder와 연결되어 있음
            - 여기서 authenticate(token)을 호출하면:
                1) CustomUserDetailsService.loadUserByUsername(username) 실행
                2) PasswordEncoder로 비밀번호 검증 자동 처리
                3) 성공 시 Authentication 객체 반환(Principal=CustomUserDetails)
         */
        try{
            // 4) 사용자 입력(username,password)로 인증 토큰 생성(인증 전)
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(normalizedUsername, rawPassword);

            // 5) 인증 수행 (실패 시 AuthenticationException 발생)
            Authentication authentication = authenticationManager.authenticate(authRequest);

            // 6) 인증 성공 -> SecurityContextHolder에 인증 정보 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 7) 마지막 로그인 시각 업데이트를 위해 User엔티티 조회
            User user = userRepository.findByUsername(normalizedUsername)
                    .orElseThrow(() -> new EntityNotFoundException("User not found. username: " + normalizedUsername));

            // 8) 마지막 로그인 시각 업데이트
            user.updateLastLoginAt(LocalDateTime.now());

            // 9) 로그인 성공 >> User엔티티 DTO 변환 후 반환
            return UserResponseDto.from(user);
        } catch (BadCredentialsException e){ // ID or PW 틀렸을 때 예외 메시지
            log.warn("로그인 실패 - 아이디 또는 비밀번호 불일치. username={}", normalizedUsername);
            throw new AuthenticationFailException("아이디 또는 비밀번호를 확인해주세요.");
        } catch (AuthenticationException e){ // 그 외 인증계열 (잠금,만료 등) 통일 처리
            log.warn("로그인 실패 - 인증 처리 중 오류. username={}, message={}", normalizedUsername, e.getMessage());
            throw new AuthenticationFailException("아이디 또는 비밀번호를 확인해주세요.");
        }
    }

    // username 중복검사 (DB확인)
    private void validateDuplicateUsername(String username){
        if (username == null || username.isBlank()){ // 기본 유효성 검사
            throw new IllegalArgumentException("ID는 필수 입력 사항입니다.");
        }
        String norm = username.trim().toLowerCase(); // 엔티티 @PrePersist와 동일 정책
        boolean exists = userRepository.findByUsername(norm).isPresent();// 중복 여부 확인
        if (exists){
            throw new IllegalArgumentException("이미 사용중인 ID 입니다 : " + username);
        }
    }

    // email 중복 검사 (DB확인)
    private void validateDuplicateEmail(String email){
        String norm = email.trim().toLowerCase(); // 엔티티 @PrePersist와 동일 정책
        boolean exists = userRepository.findByEmail(norm).isPresent(); // 중복 여부 확인
        if (exists){
            throw new IllegalArgumentException("이미 사용중인 E-mail 입니다 : "+ email);
        }
    }

    // 관리자,서버 내부 로직이 사용자를 식별할 경우, 본인 포함
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public User getById(Long id){ //PK(id)로 단건조회(Read)
        // 미 존재시 EntityNotFoundException 던지기
        return userRepository.findById(id) // Optional<User> 반환
                .orElseThrow(()->new EntityNotFoundException("User not found. id = "+id)); // 값 없으면 예외
        /*
        * 컨트롤러/예외처리 레이어에서 한글화/표준 응답 변환 예정
        * (ex : 404 상태코드와 사용자 친화 메시지로 매핑할 것)*/
    }

    // 전체 조회(Read All), 관리자용
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAll(){ //List타입으로 빈 리스트반환[]이 가능하기에 elseTrow 불필요
        return userRepository.findAll(); //모든 User 목록 반환.
    }

    // username으로 조회, 게시판 등 다른곳에서 username검색이 사용될 수 있으니 @PreAuthorize(보안)설정X
    public User getByUsername(String username){
        return userRepository.findByUsername(username) //Optional<User>
                .orElseThrow(()-> new EntityNotFoundException("User not found. username= "+username));
                //없으면 예외처리
    }

    //닉네임 변경
    @Transactional // 쓰기 트랜잭션(수정)
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") //본인 혹은 관리자만 가능
    public User updateNickname(Long id, String newNickname){ //닉네임 변경 : 유니크 중복검사 후 엔티티 위임
        User user = getById(id); // 존재 확인

        //  바꾸려는 닉네임이 기존과 다른때만 검사(트림결과 포함,불필요한 DB조회 방지)
        // 입력 정규화 : null허용X , 공백만있으면 무시, 값이 있으면 trim
        String trimmed = (newNickname == null) ? null : newNickname.trim(); //삼항연산자
        if(trimmed != null && !trimmed.isBlank() && !trimmed.equals(user.getNickname())){
            //닉네임 중복검사(자기 자신 제외)
            userRepository.findByNickname(trimmed).ifPresent(conflict -> {
                if (!conflict.getId().equals(id)) {
                    throw new IllegalStateException("이미 사용 중인 닉네임입니다: " + trimmed);
                }
            });
            user.changeNickname(trimmed); // 엔티티 상태변경
            /*내부적으로 updateProfile(trimmed, null) 위임하도록 구성*/
        }
        return user;
    }

    //이메일 변경, 유니크 중복 검사 후 엔티티 위임
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") //본인 혹은 관리자만 가능
    public User updateEmail(Long id, String newEmail){
        User user = getById(id); // 대상 사용자 존재 확인

        String trimmed = (newEmail == null) ? null : newEmail.trim();
        if (trimmed != null && !trimmed.isBlank() && !trimmed.equals(user.getEmail())) {
            userRepository.findByEmail(trimmed).ifPresent(conflict -> {
                if (!conflict.getId().equals(id)) {
                    throw new IllegalStateException("이미 사용 중인 이메일입니다: " + trimmed);
                }
            });
            user.changeEmail(trimmed); // ← 엔티티 단일 진입점 재사용
        }
        return user;
        /* 확장 포인트
        *   - 이메일 변경 시 재인증 토큰 발송/검증 흐름연계*/
    }

    //삭제
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id") //본인 혹은 관리자만
    public void delete(Long id){
        User user = getById(id);//존재 확인
        userRepository.delete(user); // 삭제 실행
        /*
            연관관계 정책 주의
                -Post 등 연관 엔티티가 생기면 orphanRemoval/CASCADE/FK 옵션 정책을 맞춰야함
                - 운영에서는 '탈퇴 처리(soft delete) 전략도 고려해볼 필요 있을듯.
         */
    }


}
