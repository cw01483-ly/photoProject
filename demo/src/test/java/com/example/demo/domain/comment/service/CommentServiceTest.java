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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Mockito 기반 단위 테스트 실행 (스프링 컨테이너 없이 @Mock/@InjectMocks 동작)
/*
    ⭐ verify       : Mockito의 기능 중 하나, 메서드가 실제로 호출 되었는지 검증
                        -> 결과 값을 보는게 아닌, 과정(행동, side effect)을 검증.
    ⭐ assertThat   : 결과 값을 보여줌
 */

public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository; // CommentService 댓글 저장소를 가짜(Mock)로 대체

    @Mock
    private UserRepository userRepository; // CommentService 사용자 저장소를 가짜(Mock)로 대체

    @InjectMocks
    private CommentService commentService; // 위 Mock 들을 주입받아 실제 CommentService 객체를 생성

    // ⭐ 권한 로직 단위 테스트: ADMIN 수정 허용
    @Test
    @DisplayName("관리자는 다른 사용자의 댓글도 수정 가능")
    void updateComment_admin_success(){
        // [GIVEN] 작성자 댓글, 요청자 세팅, Repository 스텁 구성
        Long commentId = 1L; // 수정 대상 댓글 ID
        Long authorId = 2L; // 댓글 작성자 ID
        Long adminId = 3L;
        String newContent = "수정 내용";

        User author = User.builder()
                .username("author1")
                .password("password1!")
                .nickname("nickname")
                .email("author@example.com")
                .build();
        ReflectionTestUtils.setField(author,"id",authorId); // private필드 id 강제 주입
        ReflectionTestUtils.setField(author, "role", UserRole.USER);
        // ↑ 이 테스트에서 작성자는 일반 사용자임을 명시

        User admin = User.builder()
                .username("admin1")
                .password("password1!")
                .nickname("adminNickname")
                .email("admin@example.com")
                .build();
        ReflectionTestUtils.setField(admin,"id",adminId);
        // ↑ admin 이라는 자바 객체의 id 필드에 숫자 adminId를 강제로 대입
        ReflectionTestUtils.setField(admin,"role", UserRole.ADMIN);

        Comment comment = Comment.builder()
                .author(author)
                .content("원본 내용")
                .post(null) // 수정 권한만 검증하므로 post사용 X
                .build();
        ReflectionTestUtils.setField(comment,"id",commentId);

        when(commentRepository.findByIdWithAuthor(commentId))
                .thenReturn(comment); // 서비스가 댓글+작성자를 조회하도록 스텁 설정
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(admin)); // 서비스가 userId로 사용자를 조회해 role을 확인하도록 스텁 설정

        // [WHEN] ADMIN이 다른 user의 댓글 수정 시도
        Comment result = commentService.updateComment(commentId, adminId, newContent);

        // [THEN] DB조회
        assertThat(result.getContent()).isEqualTo(newContent);
        verify(commentRepository, times(1)).findByIdWithAuthor(commentId);
        // ↑ updateComment() 실제로 호출하는건 findByIdWithAuthor
        verify(userRepository, times(1)).findById(adminId);
        // ↑ 관리자 여부 판단을 위해 requestor 조회 1번 호출되어야 함을 명시
    }



    // ⭐ 관리자 권한 단위 테스트(ADMIN): 작성자 아니어도 삭제 가능
    @Test
    @DisplayName("관리자는 다른 사용자의 댓글도 삭제 가능")
    void deleteComment_admin_success(){
        // [GIVEN] 작성자가 있는 댓글 + 요청자(ADMIN) 생성, Repository 스텁 구성
        Long commentId = 1L; // 삭제 대상 commentID
        Long authorId = 2L; // 댓글 작성자 userId
        Long adminId = 3L; // 삭제 요청자 adminId
        // 작성자 생성
        User author = User.builder()
                .username("author1")
                .password("password1!")
                .nickname("nickname")
                .email("user@example.com")
                .build();
        ReflectionTestUtils.setField(author,"id",authorId);
        ReflectionTestUtils.setField(author, "role", UserRole.USER);

        // 관리자 생성
        User admin = User.builder()
                .username("admin1")
                .password("password1!")
                .nickname("adminNickname")
                .email("admin@example.com")
                .build();
        ReflectionTestUtils.setField(admin,"id",adminId);
        ReflectionTestUtils.setField(admin,"role", UserRole.ADMIN);

        // 댓글 생성
        Comment comment = Comment.builder()
                .author(author)
                .content("원본 내용")
                .post(null)
                .build();
        ReflectionTestUtils.setField(comment,"id",commentId);// 삭제 대상 댓글 id를 테스트에서 수동 주입

        when(commentRepository.findByIdWithAuthor(commentId))
                .thenReturn(comment); // 서비스는 댓글 조회시 author가 필요 >> findByIdWithAuthor 사용
        when(userRepository.findById(adminId))
                .thenReturn(Optional.of(admin)); // 서비스는 요청자 role(ADMIN 여부) 판단을 위해 user 조회

        // [WHEN] ADMIN이 user의 댓글 삭제 시도
        commentService.deleteComment(commentId, adminId);

        // [THEN] 관리자면 삭제 허용 + delete 호출 발생 + 필요한 조회 메서드 호출 검증
        // ↓ 댓글(+작성자) 조회 1회
        verify(commentRepository, times(1)).findByIdWithAuthor(commentId);
        // ↓ 요청자 조회 1회 (관리자 여부 판단)
        verify(userRepository, times(1)).findById(adminId);
        // ↓ 관리자는 작성자가 아니여도 delete 호출
        verify(commentRepository, times(1)).delete(comment);
    }
}
