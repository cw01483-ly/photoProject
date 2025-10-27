package com.example.demo.domain.post.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// JPA프록시 생성을 위한 기본 생성자, 외부에서 호출 불가.
@AllArgsConstructor //모든 필드를 인자로 받는 생성자 자동 생성
@Builder
@ToString(exclude = "author") //author필드를 출력에서 제외(순환 참조 방지)
@EqualsAndHashCode(of = "id") // id 필드만으로 동일성 비교 (엔티티 루프 방지)
@Table(
        name = "posts", // 실제 테이블명 지정 (post는 예약어이므로 복수형 사용)
        indexes = {
                @Index(name = "idx_posts_user_id", columnList = "user_id"), // 작성자 기준 조회 인덱스
                @Index(name = "idx_posts_title", columnList = "title") // 제목 기준 검색 인덱스
        }
)
@SQLDelete(sql = "UPDATE posts SET is_deleted = true WHERE id = ?")
// delete 실행 시 실제 삭제 대신 is_deleted=true로 업데이트
@Where(clause = "is_deleted = false")
// 항상 is_deleted=false인 데이터만 조회되도록 필터링

public class Post {
}
