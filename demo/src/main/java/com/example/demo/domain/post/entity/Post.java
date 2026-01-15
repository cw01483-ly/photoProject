package com.example.demo.domain.post.entity;

import com.example.demo.domain.user.entity.User;
import com.example.demo.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// JPA프록시 생성을 위한 기본 생성자, 외부에서 호출 불가.
@ToString(exclude = "author") //author필드를 출력에서 제외(순환 참조 방지)
// author는 @ManyToOne관계(지연로딩) > 예기치 않은 DB호출로 성능저하, 재귀순환문제 생기는것을 예방
@EqualsAndHashCode(of = "id") // id 필드만으로 동일성 비교 (엔티티 루프 방지)
@Table(
        name = "posts", // 실제 테이블명 지정 (post는 예약어이므로 복수형 사용)
        indexes = {
                @Index(name = "idx_posts_user_id", columnList = "user_id"), // 작성자 기준 조회 인덱스
                @Index(name = "idx_posts_title", columnList = "title") // 제목 기준 검색 인덱스
        }
)
@Where(clause = "is_deleted = false")// 조회할 때 항상 WHERE is_deleted=false 조건을 자동으로 붙이기
/* 항상 is_deleted=false인 데이터만 조회되도록 필터링
    >> 조회할 때 삭제되지 않은(is_deleted = false)데이터만 보여줘라!
    즉 'SQLDelete'와'Where' 으로 SoftDelete 시스템 구현*/
/*
SoftDelete 의 장점 ?
    1) 실수로 삭제한다 해도 is_delete=false로 되돌리면 복원 가능
    2) 언제, 누가 삭제했는지 로그 남길 수 있음
    3) 관계 무결성 유지
    4) 통계 유지
SoftDelete 의 단점 ?
    1) 데이터의 누적 (오래된 데이터는 백업 후 물리 삭제 배치 처리할 것)
    2) 중복 문제(Unique제약 >> UNIQUE 조건에 is_deleted 포함 시키기)
    3) 복잡한 쿼리 필요함 (Where 로 자동 필터링 혹은 JPQL에서 명시적 조건 사용하기)
    */
public class Post extends BaseTimeEntity { //BaseTimeEntity를 상속하여 시간설정 자동관리
    // Post의 DB테이블에 자동으로 createAt, updatedAt 자동 생성

    @Id //Pk
    @GeneratedValue(strategy = GenerationType.IDENTITY) // INSERT시점에 PK 자동 증가,생성
    private Long id;

    @Column(nullable = false)
    private Long displayNumber; //게시글 번호 , 게시글 삭제 후 재정렬을 위해 id와 별도 생성

    //게시글 제목
    @NotBlank(message = "제목은 공백일 수 없습니다.")
    @Size(max=100,message="제목은 100자 이내로 입력해주세요")
    @Column(nullable = false, length = 100)
    private String title;

    //게시글 내용
    @NotBlank(message = "내용을 입력하세요.")
    @Column(nullable = false,columnDefinition = "TEXT") // DB컬럼 타입을 TEXT로 명시하여 대용량 텍스트를 지원.
    private String content;

    //작성자 정보
    @ManyToOne(fetch = FetchType.LAZY, optional = false) //User(작성자)와 N:1 설정(지연로딩설정,부모없으면 금지)
    @JoinColumn(name="user_id",nullable = false,foreignKey = @ForeignKey(name="fk_posts_user"))
    //외래키(FK)설정, null 허용X
    /*
        JoinColumn(name="user_id" : posts.user_id라는 외래키 컬럼을 만들어 users.id참조
        foreignKey = @ForeignKey(name="fk_posts_user" : FK 제약조건의 이름 명시
        >> Post테이블에 user_id라는 FK 컬럼을 만들고, 반드시 users.id를 참조하게 하라 == 작성자 필수)
     */
    private User author; //작성자 정보는 User엔티티를 참조

    //조회수 카운트
    @Column(nullable = false)
    private int views; //조회수의 처음값 0

    //논리 삭제 여부(false - 정상, true = 삭제)
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    // 게시글 이미지 경로(URL 또는 파일 경로)
    @Column(name = "image_path")
    private String imagePath;
    /*
                        비지니스 메서드 영역
     */

    /*
        @PrePersist : INSERT 직전 실행
        @PreUpdate : UPDATE 직전 실행 (수정 시간, 로그 갱신 등)
        @PreRemove : DELETE 직전 실행 (삭제 로그 남기기)
     */

    @PrePersist
    /*엔티티가 처음 저장(Persist)되기 전 자동으로 실행됨
        > 게시글이 처음 저장될 때 displayNumber가 지정되지 않았다면 기본값을 부여
         > 나중에 서비스 계층에서 직접 계산해서 setDisplayNumber() 호출할 수 있음*/
    public void assignDefaultDisplayNumber(){
        if(this.displayNumber==null){
            this.displayNumber = 0L; //기본값 0으로 설정(임시값)
        }
    }

    //게시글 수정 메서드
    public void update(String title, String content){
        if(title != null && !title.isBlank()) this.title = title.trim();
        if(content != null && !content.isBlank()) this.content = content.trim();
        //제목 혹은 컨텐츠가 공백이 아닐때 변경되도록 조건부 설치, 문자열의 앞뒤 공백은 제거(trim)
    }

    //삭제 수행 메서드 (실제 DB삭제 X )
    public void delete(){
        this.isDeleted=true;
        //DB에서 실제로 삭제하지 않고 isDeleted 값을 true로 변경하여 논리삭제 처리
    }

    // 게시글 이미지 변경 메서드
    public void changeImage(String imagePath) {
        this.imagePath = imagePath;
    }

    // 필요한 생성자에만 @Builder 적용
    @Builder
    private Post(
            String title,
            String content,
            User author,
            Long displayNumber,
            String imagePath){
        this.title = title;
        this.content = content;
        this.author = author;
        this.displayNumber = displayNumber;
        this.imagePath = imagePath;
        this.views = 0;
        this.isDeleted = false;
    }
}
