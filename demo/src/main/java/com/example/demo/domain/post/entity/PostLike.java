package com.example.demo.domain.post.entity;

import com.example.demo.domain.user.entity.User;
import com.example.demo.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/*
    PostLike 엔티티
    - "어떤 유저(User)가 어떤 게시글(Post)에 좋아요를 눌렀는지"를 저장하는 테이블
    - 하나의 행(row)이 "user 1명이 post 1개에 남긴 좋아요 1개"를 의미
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시 생성을 위한 기본 생성자
@AllArgsConstructor // 모든 필드를 매개변수로 받는 생성자 자동 생성
@Builder
@Table(
        name = "post_likes", // 실제 DB 테이블 이름 지정
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_likes_post_user", // 유니크 제약조건 이름
                        columnNames = {"post_id", "user_id"} // 같은 post+user 조합 한 번만 허용
                )
        },
        indexes = {
                @Index(name = "idx_post_likes_post_id", columnList = "post_id"), // 게시글 기준 좋아요 조회 인덱스
                @Index(name = "idx_post_likes_user_id", columnList = "user_id")  // 사용자 기준 좋아요 조회 인덱스
        }
)
public class PostLike extends BaseTimeEntity {

    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT (DB가 PK 값 생성)
    private Long id; // PostLike 엔티티의 고유 ID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // N:1 관계, 하나의 게시글(Post)에 여러 좋아요(PostLike)가 달릴 수 있음
    // LAZY: 실제로 접근할 때까지 Post 엔티티를 조회하지 않음(성능 최적화)
    @JoinColumn(
            name = "post_id", // FK 컬럼 이름 (post_likes.post_id)
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_likes_post") // FK 제약조건 이름 지정
    )
    private Post post; // 좋아요가 눌린 게시글

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // N:1 관계, 한 유저(User)는 여러 게시글에 좋아요를 남길 수 있음
    @JoinColumn(
            name = "user_id", // FK 컬럼 이름 (post_likes.user_id)
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_likes_user") // FK 제약조건 이름 지정
    )
    private User user; // 좋아요를 누른 사용자

    /*
        비즈니스 메서드 영역
        - 엔티티 내부에서 의미 있는 도메인 동작을 메서드로 묶어두는 곳
     */

    // 연관관계를 설정하는 편의 메서드 (필요 시 사용)
    public void setPost(Post post) {
        this.post = post; // 어떤 게시글에 대한 좋아요인지 설정
    }

    public void setUser(User user) {
        this.user = user; // 어떤 사용자가 누른 좋아요인지 설정
    }

}
