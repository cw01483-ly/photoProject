package com.example.demo.domain.user.repository;


import com.example.demo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    UserRepository
    User엔티티를 DB에 접근할 수 있도록 하는 인터페이스.
    JpaRepository를 상속받아서 CRUD기능을 자동으로 제공할 수 있음
    이 클래스는 @Repository를 생성하지 않아도
    Spring Data JPA 가 자동으로 인식하고 Bean으로 등록한다.
*/
public interface UserRepository extends JpaRepository<User, Long> {

    /*
        findByEmail
         - 이메일 주소로 사용자 조회하는 메서드
         - 로그인 or 비밀번호 찾기 등에서 사용
    */
    Optional<User> findByEmail(String email);

    /*
        existsByUsername
        - 회원가입 시 아이디(username) 중복 여부 확인
        - 존재하면 true, 없으면 false 반환
     */
    boolean existsByUsername(String username);

    /*
        existsByEmail
        - 회원가입 시 이메일 중복 여부 확인
        - 존재하면 true, 없으면 false 반환
    */
    boolean existsByEmail(String email);
}
