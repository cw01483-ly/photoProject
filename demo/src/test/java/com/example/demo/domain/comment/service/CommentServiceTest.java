package com.example.demo.domain.comment.service;

import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.domain.user.role.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Mockito 기반 단위 테스트 실행 (스프링 컨테이너 없이 @Mock/@InjectMocks 동작)
/*
    ⭐ verify       : Mockito의 기능 중 하나, 메서드가 실제로 호출 되었는지 검증
                        -> 결과 값을 보는게 아닌, 과정(행동, side effect)을 검증.
    ⭐ assertThat   : 결과 값을 보여줌
 */
/*
 CommentService 권한 로직 단위 테스트
 - ADMIN / USER 권한에 따른 댓글 수정·삭제 검증
 - Service 계층만 테스트 (Controller/HTTP 제외)
*/
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository; // CommentService 댓글 저장소를 가짜(Mock)로 대체

    @Mock
    private UserRepository userRepository; // CommentService 사용자 저장소를 가짜(Mock)로 대체

    @InjectMocks
    private CommentService commentService; // 위 Mock 들을 주입받아 실제 CommentService 객체를 생성

    // =============== 테스트 헬퍼 메서드(User엔티티 생성 + id/role 강제 주입) ===============
    private User createUser(Long id, UserRole role, String username){
        User user = User.builder()
                .username(username)
                .password("password123!")
                .nickname(username+"_"+id)
                .email(username + "@example.com")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }
    // =============== 테스트 헬퍼 메서드(Comment 엔티티 생성 + id 강제 주입) ===============
    private Comment createComment(Long commentId, User author, String content){
        Comment comment = Comment.builder()
                .author(author)
                .content(content)
                .post(null)
                .build();
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }


    // ⭐ 권한 로직 단위 테스트: ADMIN 수정 허용
    @Test
    @DisplayName("관리자는 다른 사용자의 댓글도 수정 가능")
    void updateComment_admin_success(){
        // [GIVEN] 작성자 댓글, 요청자 세팅, Repository 스텁 구성
        Long commentId = 1L; // 수정 대상 댓글 ID
        Long authorId = 2L; // 댓글 작성자 ID
        Long adminId = 3L;

        User author = createUser(authorId, UserRole.USER, "author");
        User admin = createUser(adminId, UserRole.ADMIN, "admin");
        Comment comment = createComment(commentId, author, "원본 내용");

        when(commentRepository.findByIdWithAuthor(commentId))
                .thenReturn(comment); // 서비스가 댓글+작성자를 조회하도록 스텁 설정
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(admin)); // 서비스가 userId로 사용자를 조회해 role을 확인하도록 스텁 설정

        // [WHEN] ADMIN이 다른 user의 댓글 수정 시도
        Comment result = commentService.updateComment(commentId, adminId, "수정 내용");

        // [THEN] DB조회
        assertThat(result.getContent()).isEqualTo("수정 내용");
        verify(commentRepository).findByIdWithAuthor(commentId);
        verify(userRepository).findById(adminId);
    }



    // ⭐ 권한 테스트(NOT AUTHOR, NOT ADMIN): 댓글 수정 실패
    @Test
    @DisplayName("작성자도 관리자도 아닌 사용자는 댓글 수정 불가")
    void updateComment_forbidden_notAuthorNotAdmin(){
        // [GIVEN] 작성자1, 댓글, 요청자1(USER), Repository 스텁 구성
        Long commentId = 1L;
        Long authorId = 2L;
        Long otherUserId = 3L; // 수정 요청자 (일반 유저)

        User author = createUser(authorId, UserRole.USER, "author");
        User otherUser = createUser(otherUserId, UserRole.USER, "other");
        Comment comment = createComment(commentId, author, "원본");


        when(commentRepository.findByIdWithAuthor(commentId))
                .thenReturn(comment); // 댓글+작성자 조회 스텁
        when(userRepository.findById(otherUserId))
                .thenReturn(Optional.of(otherUser)); // 요청자 조회 스텁

        // [WHEN & THEN] 권한 부족 >> 예외 발생
        assertThatThrownBy(() ->
                commentService.updateComment(commentId, otherUserId, "수정 시도")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 작성자만 댓글을 수정할 수 있습니다.");

        // [THEN] 내용 변경 없는지 확인
        assertThat(comment.getContent()).isEqualTo("원본");
    }



    // ⭐ 관리자 권한 단위 테스트(ADMIN): 작성자 아니어도 삭제 가능
    @Test
    @DisplayName("관리자는 다른 사용자의 댓글도 삭제 가능")
    void deleteComment_admin_success(){
        // [GIVEN] 작성자가 있는 댓글 + 요청자(ADMIN) 생성, Repository 스텁 구성
        Long commentId = 1L; // 삭제 대상 commentID
        Long authorId = 2L; // 댓글 작성자 userId
        Long adminId = 3L; // 삭제 요청자 adminId

        User author = createUser(authorId, UserRole.USER, "author");
        User admin = createUser(adminId, UserRole.ADMIN, "admin");
        Comment comment = createComment(commentId, author, "원본 내용");

        when(commentRepository.findByIdWithAuthor(commentId))
                .thenReturn(comment); // 서비스는 댓글 조회시 author가 필요 >> findByIdWithAuthor 사용
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(admin)); // 서비스는 요청자 role(ADMIN 여부) 판단을 위해 user 조회

        // [WHEN] ADMIN이 user의 댓글 삭제 시도
        commentService.deleteComment(commentId, adminId);

        // [THEN] 관리자면 삭제 허용 + delete 호출 발생 + 필요한 조회 메서드 호출 검증
        verify(commentRepository).findByIdWithAuthor(commentId);
        verify(userRepository).findById(adminId);
        verify(commentRepository).delete(comment); // 권한 검증 통과 시 delete 호출되어야 함
    }




    // ⭐ 권한 테스트(NOT AUTHOR, NOT ADMIN): 댓글 삭제 실패
    @Test
    @DisplayName("작성자도 관리자도 아닌 사용자는 댓글 삭제 불가")
    void deleteComment_forbidden_notAuthorNotAdmin(){
        // [GIVEN] 작성자1, 댓글1, 요청자(USER), Repository 스텁 구성
        Long commentId = 1L;      // 삭제 대상 댓글 ID
        Long authorId = 2L;       // 댓글 작성자 ID
        Long otherUserId = 3L;    // 삭제 요청자 ID

       User author = createUser(authorId, UserRole.USER, "author");
       User otherUser = createUser(otherUserId, UserRole.USER, "other");
       Comment comment = createComment(commentId, author, "원본 내용");

        when(commentRepository.findByIdWithAuthor(commentId))
                .thenReturn(comment); // 댓글(+작성자) 조회 스텁
        when(userRepository.findById(otherUserId))
                .thenReturn(Optional.of(otherUser)); // 요청자(USER) 조회 스텁

        // [WHEN & THEN] 작성자도 관리자도 아닌 사용자가 삭제 시도하면 예외 발생
        assertThatThrownBy(() ->
                        commentService.deleteComment(commentId, otherUserId)
                /* ↑ deleteComment 실행 시
                     - comment.author.id != otherUserId 이고
                     - requester.role != ADMIN 이므로
                   권한 검증 로직에서 실패*/
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 작성자만 삭제할 수 있습니다.");
        // ↑ 삭제 권한 위반 시 의도한 예외 + 메시지가 발생했는지까지 검증

        // [THEN] 조회는 발생하지만, 실제 delete는 절대 호출되면 안 됨
        verify(commentRepository).findByIdWithAuthor(commentId); // 댓글 조회 발생
        verify(userRepository).findById(otherUserId);  // 요청자 조회 발생
        verify(commentRepository, never()).delete(any()); // 권한 실패 시 delete 호출 금지
    }
}
