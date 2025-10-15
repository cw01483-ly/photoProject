package com.example.demo.global.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
/*이 클래스를 상속하는 엔티티는 createAt(생성),updatedAt(수정) 필드를
 * 자신의 컬럼으로 가지게 된다. BUT 이 클래스 자체는 테이블로 생성되지 않는다!*/
@EntityListeners(AuditingEntityListener.class)
/*스프링 데이터 JPA의 감사 리스너를 연결.
 * 엔티티가 저장/수정될 때 리스너가 개입하여
 * @CreatedDate, @LastModifiedDte 필드를 자동으로 세팅 또는 갱신!*/
public abstract class BaseTimeEntity {
    /* abstract : 이 클래스는 "상속 전용" 클래스임을 선언
    * >> new를 사용하여 인스턴스를 만들지 못하게 하고 오용을 방지한다.*/

    @CreatedDate // INSERT 시점에 스프링 데이터 JPA가 값을 자동으로 세팅
    @Column(updatable = false)
    // 생성 시각은 논리적으로 바뀌면 안됨! > UPDATE 쿼리에서 이 컬럼은 변경대상에서 제외시키기
    private LocalDateTime createdAt;
    //설계 의도. 1.외부의 임의 변경을 막기위해  2.Setter를 두지않고 Getter만 제공

    @LastModifiedDate // UPDATE 시점마다 스프링데이터 JPA가 값을 자동갱신
    private LocalDateTime updatedAt;
    //업데이트타임 필드는 감사리스너가 관리함으로 Setter없이 Getter만 제공

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getUpdatedAt(){
        return updatedAt;
    }
}
