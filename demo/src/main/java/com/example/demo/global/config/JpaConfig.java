package com.example.demo.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration //이 클래스를 '스프링 설정파일'로 인식한다.
@EnableJpaAuditing
/* JPA의 자동 날짜기록(Auditing) 기능
     > BaseTimeEntity의 @CreatedDate, LastModifiedDate 의 동작을 위해서 */
public class JpaConfig {
    /*설정 전용 클래스
        메서드나 로직이 따로 없음, 단지 JPA Auditing 기능을 활설화 시키는 역할
        @EnableJpaAuditing는 전등을 키는 스위치 역할
        CreatedDate, LastModifiedDate는 전등 역할
        엔티티에서 앱 전반의 설정을 켜버리면 책임이 섞이게 된다.
        즉 데이터의 형태와 프로그램의 환경설정이 섞이는 구조*/
}
