package com.example.demo.domain.comment.controller;

import com.example.demo.domain.comment.dto.CommentCreateRequestDto;
import com.example.demo.domain.comment.dto.CommentUpdateRequestDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc; // 가짜 HTTP 요청/응답 도구

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
    CommentControllerTest

    - 실제 HTTP 요청을 흉내 내서 CommentController 의 동작을 검증하는 통합 테스트 클래스
    - MockMvc 를 사용하여 /api/posts/{postId}/comments 엔드포인트를 호출하고
      응답(JSON)과 DB 상태를 함께 확인.
 */
@SpringBootTest
@AutoConfigureMockMvc

public class CommentControllerTest {

    private final MockMvc mockMvc; // 가짜 HTTP 요청/응답을 수행할 핵심 객체
    private final ObjectMapper objectMapper; // 자바 객체를 JSON 문자열로 변환하기 위한 Jackson 객체
    private final UserRepository userRepository; // 유저 엔티티 저장/조회용 리포지토리
    private final PostRepository postRepository; // 게시글 엔티티 저장/조회용 리포지토리
    private final CommentRepository commentRepository; // 댓글 엔티티 저장/조회용 리포지토리


    /*
        생성자 주입
        - @Autowired 생성자를 통해 필요한 의존성을 모두 주입받는다.
        - 필드를 final 로 유지, 생성 시점 이후 의존성이 변경되지 않도록 보장.
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public CommentControllerTest(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PostRepository postRepository,
            CommentRepository commentRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }
    @BeforeEach
    void setUp() {
        /*
            각 테스트 실행 "직전"에 항상 호출되는 메서드

            - 테스트 간 완벽한 격리를 위해 연관된 테이블 데이터를 모두 삭제한다.
            - 삭제 순서 중요!
                1) 댓글(Comment) : 자식 엔티티 (Post, User 를 참조)
                2) 게시글(Post)   : 부모(Author = User)를 참조하는 자식
                3) 유저(User)     : 최상위 부모 엔티티
            - 이렇게 "자식 >> 부모" 순서로 지워야 외래 키 제약 조건 위반을 피할 수 있다.
         */
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM posts");
        jdbcTemplate.execute("DELETE FROM users");
    }


    // =============== 테스트 헬퍼 메서드 (중복 GIVEN 제거) ===============
    // ⭐ User
    private User saveUser(String username, String email, String nickname){
        // User.builder()는 role을 USER로 고정 >> 일반 유저 생성시 적용
        return userRepository.save(
                User.builder()
                        .username(username)
                        .password("Password123!")
                        .email(email)
                        .nickname(nickname)
                        .build()
        );
    }
    // ⭐ Admin
    private User savAdmin(String username, String email, String nickname){
        // User.builder()는 role을 USER로 고정 >> 관리자 생성은 DB update 후 재조회 방식이 필수
        User admin = saveUser(username, email, nickname);

        jdbcTemplate.update(
                "UPDATE users SET role = ? WHERE id = ?",
                "ADMIN",
                admin.getId()
        );
        return userRepository.findById(admin.getId())
                .orElseThrow(() -> new IllegalStateException("관리자 유저 재조회 실패"));
    }
    // ⭐ Post
    private Post savePost(User author, long disPlayNumber, String title, String content){
        return postRepository.save(
                Post.builder()
                        .title(title)
                        .content(content)
                        .author(author)
                        .displayNumber(disPlayNumber)
                        .build()
        );
    }
    // ⭐ Comment
    Comment saveComment(Post post, User author, String content){
        return commentRepository.save(
                Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build()
        );
    }
    // ⭐ DTO -> JSON 직렬화 공통화
    private String toJson(Object dto) throws Exception{
        return objectMapper.writeValueAsString(dto);
    }
    // ========================================================================

    // ⭐ 댓글 생성 성공 테스트
    @Test
    @DisplayName("댓글 생성 성공 : 로그인 유저 댓글생성 시 201 Created 와 댓글 데이터가 반환")
    void createComment_success() throws Exception {
        // [GIVEN]
        User user = saveUser("commentUser1", "test@example.com", "댓글작성자");
        Post post = savePost(user, 1L, "제목", "내용");

        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("첫 번째 댓글")
                .build();

        String requestBody = toJson(requestDto);

        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());
        // [WHEN] API 호출

        mockMvc.perform(
                        post("/api/posts/{postId}/comments", post.getId()) // POST /api/posts/{postId}/comments
                                .with(user(principal)) // 로그인 유저 정보를 Security 컨텍스트에 심어줌
                                .contentType(MediaType.APPLICATION_JSON) // @RequestBody JSON 바인딩용
                                .accept(MediaType.APPLICATION_JSON) // JSON 응답 의도 명시
                                .content(requestBody)
                ).andDo(print())

                // [THEN] JSON 검증
                .andExpect(status().isCreated()) // HTTP 상태 코드가 201 Created 인지 확인
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 생성 성공"))
                .andExpect(jsonPath("$.data.id").exists()) // data.id (댓글 PK) 가 존재하는지 확인
                .andExpect(jsonPath("$.data.content").value("첫 번째 댓글"))
                .andExpect(jsonPath("$.data.postId").value(post.getId().intValue()))
                .andExpect(jsonPath("$.data.authorId").value(user.getId().intValue()));

        // [THEN] DB 검증
        // 1) 실제 DB 에서 해당 게시글에 달린 댓글 목록을 조회
        List<Comment> comments = commentRepository.findByPostId(post.getId()); // postId 기준 댓글 조회

        // 2) 댓글이 정확히 1개 생성되었는지 확인
        assertThat(comments).hasSize(1); // 리스트 크기 1

        Comment savedComment = comments.get(0); // 첫 번째(유일한) 댓글 꺼내기

        // 3) DB 에 저장된 댓글 내용 검증
        assertThat(savedComment.getContent()).isEqualTo("첫 번째 댓글"); // content 가 요청과 같은지 확인

        // 4) 댓글이 올바른 게시글에 연결되었는지 검증
        assertThat(savedComment.getPost().getId()).isEqualTo(post.getId());

        // 5) 댓글 작성자가 로그인 유저와 동일한지 검증
        assertThat(savedComment.getAuthor().getId()).isEqualTo(user.getId());
    }



    // ⭐ 댓글 생성 실패 테스트 (로그인하지 않은 사용자)
    @Test
    @DisplayName("댓글 생성 실패 - 비로그인 사용자는 401 Unauthorized가 발생")
    void createComment_fail_unauthenticated() throws Exception {
        // [GIVEN] 유저, 게시글 생성
        User user = saveUser("commentUser1", "login@example.com", "닉네임1");
        Post post = savePost(user, 1L, "제목", "내용");

        // 요청 DTO (Comment)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("비로그인 댓글")
                .build();
        String requestBody = toJson(requestDto);

        // [WHEN & THEN]
        mockMvc.perform(
                post("/api/posts/{postId}/comments", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody) // 로그인 정보 X
        )
            .andDo(print())
            .andExpect(status().isUnauthorized()); // 기대결과 401 Unauthorized
    }



    // ⭐ 댓글 생성 실패 테스트 (내용 공백)
    @Test
    @DisplayName("댓글 생성 실패 : 내용이 공백이면 400 Bad Request 발생")
    void createComment_fail_blankContent() throws Exception {
        // [GIVEN] 유저, 게시글 생성
        User user = saveUser("commentUser1", "blank@example.com", "닉네임1");
        Post post = savePost(user, 1L, "title", "content");

        //로그인 유저 principal 생성(인증)
        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());

        // 요청 DTO ( 댓글 내용 공백 )
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content(" ")
                .build();
        String requestBody = toJson(requestDto);

        // [WHEN & THEN] 유효성 검증 실패 → 400 Bad Request 기대
        mockMvc.perform(
                        post("/api/posts/{postId}/comments", post.getId())
                                .with(user(principal)) // 로그인한 사용자로 요청
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON) // JSON응답 302 방지
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isBadRequest()) // DTO 검증 실패 >> 400 응답 기대
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message") // errorMessage
                        .value("댓글 내용은 공백일 수 없습니다."))
                .andExpect(jsonPath("$.data.message") // ErrorResponse.message
                        .value("댓글 내용은 공백일 수 없습니다."));
    }



    // ⭐ 댓글 목록 조회 성공 테스트
    @Test
    @DisplayName("댓글 목록 조회 성공 : 특정 게시글 댓글 목록 200 OK로 반환")
    void getCommentsByPost_success() throws Exception {
        // [GIVEN] 유저, 게시글 생성
        User user = saveUser("commentUser1", "comment@example.com", "닉네임1");
        Post post = savePost(user, 1L, "title", "content");

        // 댓글 2개 생성
        saveComment(post, user, "첫 번째 댓글");
        saveComment(post, user, "두 번째 댓글");

        // [WHEN & THEN] GET api/posts/{postId}/comments" 호출
        mockMvc.perform(
                get("/api/posts/{postId}/comments", post.getId())
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 목록 조회 성공"))
                .andExpect(jsonPath("$.data.length()").value(2))
                // 정렬 검증 Repository ORDER BY c.id DESC
                .andExpect(jsonPath("$.data[0].content").value("두 번째 댓글"))
                .andExpect(jsonPath("$.data[1].content").value("첫 번째 댓글"))
                // 게시글 ID 매핑 검증
                .andExpect(jsonPath("$.data[0].postId").value(post.getId().intValue()))
                .andExpect(jsonPath("$.data[1].postId").value(post.getId().intValue()));
    }



    // ⭐ 존재하지 않는 게시글 ID로 댓글 조회 시 404 NOT FOUND가 반환
    @Test
    @DisplayName("댓글 조회 실패 : 없는 게시글, 404 반환")
    void getCommentsByPost_notFound() throws Exception{
        // [GIVEN] DB에 존재하지 않는 POST ID 지정
        Long invalidPostId = 9999999L;

        // [WHEN & THEN]
        mockMvc.perform(
                get("/api/posts/{postId}/comments", invalidPostId)// 댓글 조회 요청
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isNotFound()) // 404 ENTITY NOT FOUND
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message")
                        .value("게시글을 찾을 수 없습니다. id=" + invalidPostId))
                .andExpect(jsonPath("$.data.message")// data(ErrorResponse) 내부 message
                        .value("게시글을 찾을 수 없습니다. id=" + invalidPostId));
    }



    // ⭐ 댓글 수정 성공 테스트 (작성자 본인 댓글 내용을 수정)
    @Test
    @DisplayName("댓글 수정 성공 : 작성자가 자신의 댓글 내용을 수정, 200 OK와 수정된 댓글 데이터가 반환")
    void updateComment_success() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 1개 생성
        User user = saveUser("username1", "email@example.com", "nickname");
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "수정 전");

        // 로그인 유저 principal (작성자 본인)
        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());

        // 수정 요청 DTO (댓글 내용만 바뀐다고 가정)
        CommentUpdateRequestDto requestDto = CommentUpdateRequestDto.builder()
                .content("수정 후")
                .build();
        String requestBody = toJson(requestDto);

        // [WHEN & THEN] 댓글 수정 API 호출
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal)) //작성자 본인 요청
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(comment.getId().intValue()))
                // ↑ 댓글 id가 DB에 저장된 comment id와 동일한지 검증
                .andExpect(jsonPath("$.data.postId").value(post.getId().intValue()))
                // ↑  응답 JSON postId 가 올바르게 매핑되었는지 확인
                .andExpect(jsonPath("$.data.authorId").value(user.getId().intValue()))
                // ↑  작성자 authorId 가 올바르게 매핑되었는지 확인
                .andExpect(jsonPath("$.data.content").value("수정 후"));

        // [THEN] DB 검증
        Comment updatedComment = commentRepository.findByIdWithAuthor(comment.getId());

        // 실제 DB에도 내용이 수정되었는지 확인
        assertThat(updatedComment.getContent()).isEqualTo("수정 후");
        assertThat(updatedComment.getAuthor().getId()).isEqualTo(user.getId());
        assertThat(updatedComment.getPost().getId()).isEqualTo(post.getId());
    }



    // ⭐ 댓글 수정 실패 테스트
    @Test
    @DisplayName("댓글 수정 실패 : 비로그인 사용자는 401 Unauthorized가 발생")
    void updateComment_Fail_unauthenticated() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = saveUser("username1", "email@example.com", "nickname");
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "수정 전");

        // 비로그인 상태에서 보낼 수정 요청 본문 (JSON)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("비로그인 수정 시도")
                .build();
        String requestBody = toJson(requestDto);

        // [WHEN & THEN] 로그인 없이 PATCH 요청 >> 401 기대
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())// 댓글 수정 URL
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody)// .with(user(principal)) X : 비로그인 상태
        )
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 401 Unauthorized
    }




    // ⭐ 댓글 수정 실패 테스트
    @Test
    @DisplayName("댓글 수정 실패 : 작성자가 아닌 사용자가 댓글을 수정시 400 Bad Request 발생")
    void updateComment_Fail_notAuthor() throws Exception{
        // [GIVEN] 작성자 유저, 다른 유저, 게시글, 댓글 1개 생성
        // 댓글 작성자
        User author = saveUser("author1", "author@example.com", "작성자");
        // 수정 시도자 (다른 사용자)
        User otherUser = saveUser("user2", "other@example.com", "다른유저");

        Post post = savePost(author, 1L, "title", "content");
        Comment comment = saveComment(post, author, "원본 댓글");

        // 작성자 author 가 아닌 otherUser로 로그인
        TestUserDetails principal =
                userPrincipal(otherUser.getId(), otherUser.getUsername());

        // 수정 요청 DTO (내용 변경)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("수정 시도")
                .build();
        String requestBody = toJson(requestDto);

        // [WHEN & THEN] otherUser 가 author의 댓글 수정 API 호출 >> 400 기대
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal)) // otherUser
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andDo(print())
                .andExpect(status().isBadRequest()) //400 Bad Request
                .andExpect(jsonPath("$.success").value(false))// ApiResponse.success = false
                .andExpect(jsonPath("$.message")
                        .value("댓글 작성자만 댓글을 수정할 수 있습니다."))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글 작성자만 댓글을 수정할 수 있습니다."));
    }



    // ⭐ 댓글 수정 실패 테스트
    @Test
    @DisplayName("댓글 수정 실패 : 내용이 공백이면 400 Bad Request가 발생")
    void updateComment_fail_blankContent() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = saveUser("username1", "blank2@example.com", "nickname");
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "수정 전");


        // 로그인 유저 principal
        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());

        // 요청 DTO
        CommentUpdateRequestDto requestDto = CommentUpdateRequestDto.builder()
                .content(" ")
                .build();
        String requestBody = toJson(requestDto);

        // [WHEN & THEN] 공백 내용으로 수정 요청
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal)) // 로그인 작성자 본인
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andDo(print())
                .andExpect(status().isBadRequest()) // DTO @NotBlank 검증 실패
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("댓글 내용은 공백일 수 없습니다."))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글 내용은 공백일 수 없습니다."));
    }




    // ⭐댓글 삭제 성공 테스트
    @Test
    @DisplayName("댓글 삭제 성공 : 작성자가 자신의 댓글을 삭제하면 200 OK와 성공 메시지 반환")
    void deleteComment_success() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = saveUser("username1", "delete@example.com", "nickname");
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "삭제 전");


        // 로그인 유저 principal
        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());
        Long commentId = comment.getId(); // 삭제 대상 댓글ID

        // [WHEN & THEN] 댓글 삭제 API 호출
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .with(user(principal)) // 로그인(작성자 본인) 정보 주입
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 삭제 성공"));

        // [THEN] DB 검증
        boolean exists = commentRepository.findById(commentId).isPresent();
        // @SQLDelete + @Where(is_deleted = false) 로 인해 논리 삭제된 댓글은 조회결과에서 제외
        assertThat(exists).isFalse(); // 조회되지 않아야 함
    }



    // ⭐ 댓글 삭제 실패 테스트
    @Test
    @DisplayName("댓글 삭제 실패 : 비로그인 사용자는 401 Unauthorized가 발생")
    void deleteComment_fail_unauthenticated() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = saveUser("username1", "del401@example.com", "nickname");
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "삭제 전");

        Long commentId = comment.getId(); // 삭제 시도할 댓글 ID

        // [WHEN & THEN] 로그인 정보 없이 DELETE 요청 → 401 기대
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .accept(MediaType.APPLICATION_JSON)
                // .with() 없이 >> 비로그인 상태
        )
                .andDo(print())
                .andExpect(status().isUnauthorized()); // HTTP 401 Unauthorized
    }




    // ⭐ 댓글 삭제 실패 테스트
    @Test
    @DisplayName("댓글 삭제 실패 : 다른 사용자가 댓글 삭제 요청시 400 Bad Request가 발생")
    void deleteComment_fail_notAuthor() throws Exception{
        // [GIVEN] 유저2, 게시글, 댓글 생성
        // 댓글 작성자
        User author = saveUser("author1", "author_del@example.com", "작성자");
        // 삭제 시도자 (다른 사용자)
        User otherUser = saveUser("user2", "other_del@example.com", "다른유저");

        Post post = savePost(author, 1L, "title", "content");
        Comment comment = saveComment(post, author, "삭제 대상 댓글");
        Long commentId = comment.getId(); //삭제 시도할 commentID

        // otherUser 로 로그인 설정
        TestUserDetails principal = userPrincipal(otherUser.getId(), otherUser.getUsername());

        // [WHEN & THEN] user2 계정으로 댓글 삭제 요청
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isBadRequest())// IllegalArgumentException  400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("댓글 작성자만 삭제할 수 있습니다."))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글 작성자만 삭제할 수 있습니다."));
    }




    // ⭐ 댓글 삭제 실패 테스트
    @Test
    @DisplayName("댓글 삭제 실패 : 존재하지 않는 댓글 ID 삭제 시 400 Bad Request 반환")
    void deleteComment_fail_notFoundComment() throws Exception {
        // [GIVEN] 유저 1명 생성 (로그인용)
        User user = saveUser("testUser1", "notfound_del@example.com", "닉네임1");

        // ★ 로그인 principal 생성
        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());

        // 존재하지 않는 댓글 ID 지정
        Long invalidCommentId = 9999999L;

        // [WHEN & THEN] 존재하지 않는 댓글 삭제 시도 → 400 Bad Request
        mockMvc.perform(
                        delete("/api/comments/{commentId}", invalidCommentId)
                                .with(user(principal)) // 로그인한 사용자로 요청
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isBadRequest())   // IllegalArgumentException 400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("댓글을 찾을 수 없습니다. id=" + invalidCommentId))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글을 찾을 수 없습니다. id=" + invalidCommentId));
    }



    // ⭐ 댓글 삭제(Soft Delete) 후 댓글 목록 조회 시 삭제된 댓글은 조회 결과에서 제외
    @Test
    @DisplayName("댓글 Soft Delete 검증 : 댓글 삭제 후 게시글 댓글 목록 조회 시 삭제된 댓글이 보이지 않는다")
    void deleteComment_then_getComments_shouldNotIncludeDeletedComment() throws Exception {
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = saveUser("username1", "softdel@example.com", "nickname");
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "삭제 전");

        Long postId = post.getId();
        Long commentId = comment.getId();

        // 로그인 principal
        TestUserDetails principal = userPrincipal(user.getId(), user.getUsername());


        // [WHEN-1] 댓글 삭제 요청 ( Soft Delete )
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .with(user(principal)) // 작성자 본인 삭제
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 삭제 성공"));

        // [WHEN-2] 삭제 후 댓글 목록 조회
        mockMvc.perform(
                get("/api/posts/{postId}/comments", postId) // 해당 게시글 속 댓글 조회
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0)); // 댓글 갯수 0

        // [THEN] DB 검증
        List<Comment> comments = commentRepository.findByPostId(postId);
        assertThat(comments).hasSize(0);
        // findByPostId도 @Where가 적용되므로 삭제된 댓글은 조회되지 않아야 정상
    }



    // ⭐ 관리자 권한 테스트
    @Test
    @DisplayName("관리자 권한 테스트 : 댓글 수정 성공")
    void updateComment_success_byAdmin() throws Exception {
        // [GIVEN] 작성자, 관리자, 게시글, 댓글 생성
        User user = saveUser("user1", "user1@example.com", "작성자");

        // 관리자 유저 생성 (USER -> ADMIN으로 DB 직접 변경)
        User admin = savAdmin("admin1", "admin1@example.com", "관리자");

        // 게시글 + 댓글 (댓글 작성자는 user)
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "수정 전");


        // 관리자 principal (ROLE_ADMIN)
        TestUserDetails principal = adminPrincipal(admin.getId(), admin.getUsername());

        // 수정 요청 DTO
        CommentUpdateRequestDto requestDto = CommentUpdateRequestDto.builder()
                .content("관리자 수정")
                .build();

        // [WHEN & THEN] 관리자 계정으로 user 댓글 수정 요청
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(requestDto))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 수정 성공"))
                .andExpect(jsonPath("$.data.id").value(comment.getId().intValue()))
                .andExpect(jsonPath("$.data.authorId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.content").value("관리자 수정"));

        // [THEN] DB 검증
        Comment updateComment = commentRepository.findById(comment.getId())
                .orElseThrow(() -> new IllegalStateException("댓글이 DB에 존재하지 않습니다."));
        assertThat(updateComment.getContent()).isEqualTo("관리자 수정");
    }




    // ⭐ 관리자 권한 테스트
    @Test
    @DisplayName("관리자 권한 테스트 : 댓글 삭제 성공")
    void deleteComment_success_byAdmin() throws Exception {
        // [GIVEN] 작성자, 관리자, 게시글, 댓글 생성
        User user = saveUser("user1", "user1@example.com", "작성자");

        // ★ 관리자 유저 생성 (USER → ADMIN)
        User admin = savAdmin("admin1", "admin1@example.com", "관리자");

        // 게시글 + 댓글 (댓글 작성자는 user)
        Post post = savePost(user, 1L, "title", "content");
        Comment comment = saveComment(post, user, "삭제 전");

        Long commentId = comment.getId();// 삭제 대상 댓글 id

        // 관리자 principal
        TestUserDetails principal = adminPrincipal(admin.getId(), admin.getUsername());


        // [WHEN * THEN] 관리자 계정으로 user댓글 삭제 요청
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .with(user(principal))
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글 삭제 성공"));

        // [THEN] DB 검증
        boolean exists = commentRepository.findById(commentId).isPresent();
        assertThat(exists).isFalse();
    }









    /*
        테스트용 UserDetails 구현체

        - 실제 애플리케이션에서 사용하는 CustomUserDetails 대신,
          테스트 안에서만 사용할 간단한 구현체.
        - @AuthenticationPrincipal(expression = "id") 는
          Authentication.getPrincipal() 객체에서 getId() 를 찾아 호출하므로,
          여기서 Long id 필드를 두고 getId() 메서드를 만듦.
     */
    static class TestUserDetails implements UserDetails {

        private final Long id;
        private final String username;
        private final String password;
        private final List<? extends GrantedAuthority> authorities; // 권한 목록

        public TestUserDetails(Long id,
                               String username,
                               String password,
                               List<? extends GrantedAuthority> authorities) {
            this.id = id;                     // PK 초기화
            this.username = username;         // 아이디 초기화
            this.password = password;         // 비밀번호 초기화
            this.authorities = authorities;   // 권한 목록 초기화
        }

        public Long getId() { return id; } // @AuthenticationPrincipal(expression = "id") 에서 참조할 값

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
        // 유저의 권한 목록 반환

        @Override
        public String getPassword() { return password; } // 비밀번호 반환

        @Override
        public String getUsername() {  return username;  } // 아이디 반환

        @Override
        public boolean isAccountNonExpired() { return true; } // 계정 만료 여부 (테스트에서는 항상 true)

        @Override
        public boolean isAccountNonLocked() { return true; } // 계정 잠김 여부 (테스트에서는 항상 true)

        @Override
        public boolean isCredentialsNonExpired() { return true; } // 비밀번호 만료 여부 (테스트에서는 항상 true)

        @Override
        public boolean isEnabled() { return true; } // 계정 활성화 여부 (테스트에서는 항상 true)
    }

    //  관리자 Principal 생성 헬퍼
    private TestUserDetails adminPrincipal(Long id, String username) {
        return new TestUserDetails(
                id,
                username,
                "Password123!",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    //  일반 USER Principal 생성 헬퍼
    private TestUserDetails userPrincipal(Long id, String username) {
        return new TestUserDetails(
                id,
                username,
                "Password123!",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
