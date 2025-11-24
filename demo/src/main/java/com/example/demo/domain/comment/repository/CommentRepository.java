package com.example.demo.domain.comment.repository;

import com.example.demo.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*      CommentRepository
        - Comment 엔티티에 대한 CRUD 기능
        - JpaRepository<Comment, Long>을 상속 > 기본CRUD+페이징 자동 생성
        - Soft Delete(@Where) 적용하여 삭제된 데이터는 자동 제외
*/
public interface CommentRepository extends JpaRepository<Comment, Long> {
    // interface 선언 + JpaRepository 상속 (Comment 엔티티, PK 타입 Long)

    /*      게시글 기준으로 댓글 목록 조회
        - 특정 Post ID에 달린 댓글만 조회
        - 삭제되지 않은 데이터만 조회됨(@Where 덕분)
    */
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.id DESC")
    List<Comment> findByPostId(@Param("postId")Long postId);

    /*   특정 작성자의 모든 댓글 조회
        - User ID로 필터링
        - 마찬가지로 Soft Delete가 자동 적용됨
    */
    @Query("SELECT c FROM Comment c WHERE c.author.id = :userId ORDER BY c.id DESC")
    List<Comment> findByAuthorId(@Param("userId") Long userId);
    // userId로 댓글 목록을 조회하는 메서드
}
