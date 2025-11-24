package com.example.demo.domain.comment.service;

import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor //final 선언 필드를 매개변수로 받는 생성자를 자동으로 생성
@Transactional(readOnly = true) //이 클래스의 메서드는 "읽기 전용 트랜잭션"으로 동작

/*       CommentService
    1) 댓글 생성 (createComment)
    2) 특정 게시글의 댓글 목록 조회 (getCommentsByPost)
    3) 특정 사용자가 작성한 댓글 목록 조회 (getCommentsByAuthor)
    4) 댓글 단건 조회 (getComment)
    5) 댓글 수정 (updateComment)
    6) 댓글 삭제 (deleteComment) -> 실제로는 Soft Delete(@SQLDelete)로 논리 삭제
*/
public class CommentService {

    private final CommentRepository commentRepository; //댓글 DB작업 담당 리포지토리
    private final PostRepository postRepository;  // 댓글이 달릴 게시글 찾는 리포지토리
    private final UserRepository userRepository; // 댓글 작성자 찾는 리포지토리

    /*
        댓글 생성 메서드
        파라미터
        - postId : 게시글 ID
        - userId : 사용자 ID
        - content : 댓글 내용

        반환값
        - 생성/저장된 Comment 엔티티 객체
     */
    @Transactional //데이터 변경되는 메서드 >> readOnly=false 트랜잭션으로 동작
    public Comment createComment(Long postId, Long userId, String content){
        // 1) 댓글 달릴 게시글(post) 조회(없으면 예외)
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        // 2) 댓글 작성자(유저) 조회 (없으면 예외)
        User author = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("작성자를 찾을 수 없습니다. id=" + userId));

        // 3) Comment 엔티티 생성 (빌더 사용)
        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build();
        // 4) DB 저장 후, 저장된 엔티티 반환
        return commentRepository.save(comment);
    }

    /*
        특정 게시글에 달린 댓글 목록 조회
        파라미터
        - postId : 조회하려는 게시글의 ID
        반환값
        - 해당 게시글에 달린 댓글 목록 (List<Comment>)
     */
    public List<Comment> getCommentsByPost(Long postId) {

        // 1) 우선 게시글이 실제로 존재하는지 확인 (없으면 예외)
        postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        // 2) CommentRepository 에서 postId 기준으로 댓글 목록 조회
        //    Soft Delete(@Where(is_deleted = false)) 덕분에 삭제된 댓글은 자동으로 제외됨
        return commentRepository.findByPostId(postId);
    }

    /*
        특정 사용자가 작성한 댓글 목록 조회
        파라미터
        - userId : 댓글 작성자의 ID
        반환값
        - 해당 사용자가 작성한 댓글 목록 (List<Comment>)
     */
    public List<Comment> getCommentsByAuthor(Long userId) {

        // 1) 유저가 실제로 존재하는지 확인 (없으면 예외)
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        // 2) CommentRepository 에서 authorId 기준으로 댓글 목록 조회
        return commentRepository.findByAuthorId(userId);
    }

    /*
        댓글 단건 조회
        파라미터
        - commentId : 조회하려는 댓글의 ID
        반환값
        - 해당 ID의 Comment 엔티티 (없으면 예외 발생)
     */
    public Comment getComment(Long commentId) {

        // ID 기준으로 댓글 조회, 없으면 예외 발생
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));
    }

    // 댓글 수정
    @Transactional
    public Comment updateComment(Long commentId, Long userId, String newContent) {

        // 1) 수정할 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));

        // 2) 작성자 본인인지 검증 (작성자가 아니면 예외)
        if (!comment.getAuthor().getId().equals(userId)) {
            // 작성자가 아닌 사용자가 수정 시도할 경우 예외 발생
            throw new IllegalArgumentException("댓글 작성자만 댓글을 수정할 수 있습니다.");
        }

        // 3) 엔티티의 updateContent 메서드 호출 (내용 변경)
        comment.updateContent(newContent);

        // 4) @Transactional 상태라 save 필요 없음, 변경 감지로 자동 반영
        return comment;
    }

    /*
        댓글 삭제 메서드
        파라미터
        - commentId : 어느 댓글을 삭제할지 (댓글 ID)
        반환값
        - 없음 (void)
        특징
        - Comment 엔티티에 @SQLDelete가 걸려있다면
          실제 DB에서 DELETE 가 아니라 is_deleted = true 로 UPDATE 되는 "논리 삭제"가 실행됨
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {

        // 1) 삭제할 댓글을 조회 (없으면 예외)
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다. id=" + commentId));

        // 2) 작성자 본인 검증
        if (!comment.getAuthor().getId().equals(userId)){
            throw new IllegalArgumentException("댓글 작성자만 삭제할 수 있습니다.");
        }
        /* 3) 레포지토리의 delete 메서드 호출
              @SQLDelete 설정 덕분에 실제로는
              UPDATE comments SET is_deleted = true WHERE id = ?
              와 같은 쿼리가 실행됨
        */
        commentRepository.delete(comment);
    }
}
