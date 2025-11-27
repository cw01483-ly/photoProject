package com.example.demo.domain.post.repository;

import com.example.demo.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; 

/*
    PostLikeRepository
    - PostLike 엔티티에 대한 DB 접근(CRUD)을 담당하는 Spring Data JPA 리포지토리
    - "어떤 유저가 어떤 게시글에 좋아요를 눌렀는지"를 조회/저장/삭제할 때 사용
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    /*
        existsByPostIdAndUserId
        - 특정 게시글(postId)에 특정 사용자(userId)가 이미 좋아요를 눌렀는지 여부 확인
        - "토글" 기능 구현 시, 이미 존재하면 삭제, 없으면 새로 생성하는 분기 처리에 사용
     */
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /*
        findByPostIdAndUserId
        - 특정 게시글 + 특정 사용자 조합에 해당하는 좋아요 1개를 조회
        - 토글 기능에서 "이미 눌렀으면 그 엔티티를 삭제"할 때 사용
     */
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    /*
        countByPostId
        - 특정 게시글(postId)에 달린 전체 좋아요 개수 조회
        - 게시글 상세/목록에서 "좋아요 개수"를 보여줄 때 사용
     */
    long countByPostId(Long postId);

}
