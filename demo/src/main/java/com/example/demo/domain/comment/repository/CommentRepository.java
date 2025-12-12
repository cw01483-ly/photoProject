package com.example.demo.domain.comment.repository;

import com.example.demo.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /*      페이징 가능한 댓글 조회 메서드
        - 특정 Post ID에 달린 댓글을 "페이지 단위"로 조회
        - 파라미터
            postId : 어떤 게시글의 댓글을 조회할지
            pageable : page 번호, size(한 페이지 크기), 정렬 기준을 담은 객체
        - 반환값
            Page<Comment> : 댓글 목록(List) + 전체 페이지 수, 전체 개수 등의 메타정보 포함
        - 메서드 이름 규칙 기반 쿼리 생성
            findByPostId(...) 이름 덕분에
            자동으로 WHERE c.post.id = :postId 조건이 붙고,
            Pageable에 설정한 정렬 기준으로 ORDER BY 가 적용
    */
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    /*  ⭐ 추가: 댓글 단건 조회 시 작성자(author)를 JOIN FETCH로 같이 조회
       - 문제 원인:
           CommentResponseDto.from(comment) 내부에서 comment.getAuthor().getNickname() 호출 시
           author가 LAZY 프록시 상태인데, 컨트롤러/DTO 변환 시점엔 영속성 컨텍스트(Session)가 닫혀
           LazyInitializationException 발생
       - 해결:
           댓글을 조회할 때부터 author를 "즉시 함께 로딩" 해두면(Join Fetch)
           DTO 변환 시점에 getAuthor().getNickname()을 안전하게 호출 가능
       - 사용처(예시):
           CommentService에서 update/delete 전에
           commentRepository.findByIdWithAuthor(commentId) 로 조회해서 사용
   */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.id = :commentId")
    Comment findByIdWithAuthor(@Param("commentId") Long commentId);
    // ⭐ 추가된 메서드 (commentId로 댓글 + 작성자까지 같이 조회)

    /* ⭐ [추가] 게시글 기준 댓글 + 작성자(author)를 함께 조회 (Fetch Join)
   - LazyInitializationException 방지
   - CommentResponseDto에서 author.nickname 접근 가능
*/
    @Query("""
    SELECT c
    FROM Comment c
    JOIN FETCH c.author
    WHERE c.post.id = :postId
    ORDER BY c.id DESC
""")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);

}
