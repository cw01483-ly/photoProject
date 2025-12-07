package com.example.demo.global.security;

import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/*
    CustomUserDetailsService
     스프링 시큐리티가 로그인할 때 반드시 호출하는 서비스
     username 을 기반으로 DB에서 사용자(User) 엔티티를 조회
     조회된 User 엔티티를 CustomUserDetails 로 변환하여 반환
     SecurityContext 에 저장될 principal 정보의 기반
     principal.id 사용 가능하게 만드는 필수 구성 요소
*/
@Slf4j
@Service // 스프링 컴포넌트 등록 >> SecurityConfig 에 자동 주입 가능
@RequiredArgsConstructor // final 필드 기반 생성자 자동 생성
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; // DB에서 사용자 조회

    /*
        loadUserByUsername()
         -AuthenticationManager(로그인 처리기)가 내부적으로 호출
         -username 을 전달하면, DB에서 사용자 정보를 가져와야 함
         -UserDetails(우리가 만든 CustomUserDetails) 를 반환하면
          스프링 시큐리티의 인증 과정이 정상적으로 진행됨
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("🔍 로그인 시도 - username: {}", username);

        // 1) username 으로 User 엔티티 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("❌ 로그인 실패 - 존재하지 않는 사용자: {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        // 2) User 엔티티 → CustomUserDetails 변환
        //    이제 principal.id 가 정상적으로 제공됨!
        return new CustomUserDetails(user);
    }
}
