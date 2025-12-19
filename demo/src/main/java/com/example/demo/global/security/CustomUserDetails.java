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
        User 엔티티를 기반으로 CustomUserDetails 객체를 만드는 생성자
        - 로그인 시 UserDetailsService에서 new CustomUserDetails(user) 형태로 사용됨
        - User 엔티티에는 role이 Enum(UserRole) 이므로
          스프링 권한 규칙인 "ROLE_권한명" 으로 바꿔 SimpleGrantedAuthority 로 감싸야 한다.
     */
    public CustomUserDetails(User user) {
        this.id = user.getId();                // User PK -> principal.id
        this.username = user.getUsername();    // User(username)
        this.password = user.getPassword();    // 암호화된 비밀번호

        /*
            권한 설정
            예) UserRole.ADMIN -> "ROLE_ADMIN"
            예) UserRole.USER  -> "ROLE_USER"
            이 구현에서는 권한 문자열을 "ROLE_" + 권한명 형태로 구성하여 GrantedAuthority로 등록
         */
        this.authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }


    // UserDetails 인터페이스 구현부, 스프링 시큐리티가 로그인/세션 유지에 사용하는 기능들


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities; // 권한 목록(1개 이상)
    }

    @Override
    public String getPassword() {
        return password; // AuthenticationManager가 비밀번호 검증에 사용
    }

    @Override
    public String getUsername() {
        return username; // 로그인 아이디
    }

    /*
        아래 4개의 값은 "계정 상태"를 의미.
        true → 계정 활성
        false → 계정 제한
        지금 단계에서는 모두 true 로 설정해도 충분함.
     */

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 여부
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정 잠김 여부
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비밀번호 만료 여부
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 활성 여부
    }
}
