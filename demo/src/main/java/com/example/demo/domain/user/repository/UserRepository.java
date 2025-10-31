package com.example.demo.domain.user.repository;


import com.example.demo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/*
    UserRepository
    User엔티티를 DB에 접근할 수 있도록 하는 인터페이스.
    JpaRepository를 상속받아서 CRUD기능을 자동으로 제공할 수 있음
    이 클래스는 @Repository를 생성하지 않아도
    Spring Data JPA 가 자동으로 인식하고 Bean으로 등록한다.
*/
public interface UserRepository extends JpaRepository<User, Long> {

//기본 조회 메서드(정확히 일치하는 값 검색하기)
    /*
        findByUsername (로그인 검증)
        - username 컬럼으로 사용자 조회
        -Optional<User>로 반환하여 값이 없을때 NullPointerException 방지.
    */
    Optional<User> findByUsername(String username);

    /*
        findByEmail
         - 이메일 주소로 사용자 조회하는 메서드
         - 로그인 or 비밀번호 찾기 등에서 사용
    */
    Optional<User> findByEmail(String email);


    /*
        findByNickname
         - 닉네임 중복 검사
    */
    Optional<User> findByNickname(String nickname);


//중복 여부 검사 메서드(회원가입 시 사전 검증용)
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


//대소문자 무시(IgnoreCase) 지원 버전
/*MySQL, PostgreSQL 등 DB마다 문자열 비교 할 때 대소문자 구분 설정이 다르기에
 실무에서 입력값의 대소문자 차이로 인해 중복검사가 누락되지 않도록 lower() 함수를 사용해서 비교하는 쿼리 추가.
*/
/*@Query : Spring Data JPA가 자동으로 SQL을 만들지 않고 직접 JPQL을 작성
        "SELECT (COUNT(u) > 0)" : 조건을 만족하는 레코드 수가 0보다 크면 true, 아니면 false 반환
        FROM User u : SQL 테이블 이름이 아닌 JPA엔티티 이름을 사용해야함(User 클래스명 기준)
        WHERE LOWER(u.username) : username 컬럼을 모두 소문자로 변환하여 비교(대소문자 무시 처리)
        = LOWER(?1) : 파라미터 바인딩 , ?1은 메서드의 첫번째 인자(String username)를 의미한다.
        boolean 이 반환타입이기에 true/false 형태로 반환되므로 COUNT(u)>0 식을 사용한다.
*/

    /*
        existsByUsernameIgnoreCase
        - DB의 username 컬럼을 소문자로 변환 > 입력값과 비교.
        - User01과 user01 을 동일하게 인식.
    */
    @Query("SELECT (COUNT(u) > 0) FROM User u WHERE LOWER(u.username) = LOWER(?1)")
    boolean existsByUsernameIgnoreCase(String username);

    /*
        existsByEmailIgnoreCase
        - 이메일 주소는 대소문자 구문이 없기때문에 이를 보장하기 위해 lower() 비교적용
        - 회원가입 및 비밀번호 찾기에 안전하게 사용됨.
    */
    @Query("SELECT (COUNT(u) > 0) FROM User u WHERE LOWER(u.email) = LOWER(?1)")
    boolean existsByEmailIgnoreCase(String email);

}
