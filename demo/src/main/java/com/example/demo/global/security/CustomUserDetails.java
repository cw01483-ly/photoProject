package com.example.demo.global.security;
/* 패키지 구성
   - global.security : 인증/인가 관련 클래스들을 모아두는 패키지
*/

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.role.UserRole; // UserRole enum 사용
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/*
    CustomUserDetails
    -----------------
    - 스프링 시큐리티에서 "로그인된 사용자 정보(principal)"를 표현하는 클래스
    - 반드시 UserDetails 인터페이스를 구현해야 스프링이 인증 객체로 취급함
    - 여기서 가장 중요한 포인트는:
        ★ principal.id 값 제공 → @PreAuthorize("#id == principal.id") 를 가능하게 함
*/
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id; // ★ User PK  권한 검사에서 principal.id 로 접근 가능
    private final String username; // 로그인 아이디 (User.username)
    private final String password; // 인코딩된 비밀번호
    private final Collection<? extends GrantedAuthority> authorities; // 권한 목록 (ROLE_USER 등)

    /*
        생성자
        - User 엔티티를 기반으로 CustomUserDetails 객체를 만들기 위한 초기화
        - 스프링 시큐리티는 UserDetails를 principal로 저장하므로,
          필요한 데이터(id, username, password, authorities)를 모두 여기 저장해야 한다.
     */
    public CustomUserDetails(Long id,
                             String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = id;                // principal.id 값
        this.username = username;    // principal.username
        this.password = password;    // 로그인 시 패스워드 비교용
        this.authorities = authorities; // ROLE_USER / ROLE_ADMIN 권한 목록
    }

    /*
        정적 메서드: User 엔티티 → CustomUserDetails 변환
        ------------------------------------------------
        - User 엔티티에는 role 필드가 UserRole enum 형태로 존재
        - Spring Security에서는 "ROLE_권한" 형태를 요구하므로
          SimpleGrantedAuthority("ROLE_" + role.name()) 로 변환해야 한다.
     */
    public static CustomUserDetails from(User user) {

        // 1) 엔티티에서 user.role 가져오기 (예: UserRole.USER)
        UserRole role = user.getRole();

        // 2) 스프링 시큐리티 권한 객체로 변환
        //    UserRole.USER → "ROLE_USER"
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + role.name());

        // 3) CustomUserDetails 생성 후 반환
        return new CustomUserDetails(
                user.getId(),             // principal.id
                user.getUsername(),       // principal.username
                user.getPassword(),       // 인코딩된 비밀번호
                List.of(authority)        // 단일 권한만 있다고 가정 → 리스트 하나 생성
        );
    }

    // UserDetails 인터페이스 필수 구현 메서드

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 사용자 권한 목록 반환
        return authorities;
    }

    @Override
    public String getPassword() {
        // 로그인 시 패스워드 비교에 사용
        return password;
    }

    @Override
    public String getUsername() {
        // 로그인 식별자로 사용할 username 반환
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        // 계정 만료 여부 (true: 만료되지 않음)
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠김 여부 (true: 잠기지 않음)
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // 비밀번호 만료 여부 (true: 만료되지 않음)
        return true;
    }

    @Override
    public boolean isEnabled() {
        // 계정 활성화 상태 여부 (true: 활성화됨)
        return true;
    }
}
