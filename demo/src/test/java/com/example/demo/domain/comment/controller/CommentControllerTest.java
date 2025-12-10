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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc; // 가짜 HTTP 요청/응답 도구
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
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
        commentRepository.deleteAll(); // 모든 댓글 삭제
        postRepository.deleteAll();    // 모든 게시글 삭제
        userRepository.deleteAll();    // 모든 유저 삭제
    }


    // ⭐ 댓글 생성 성공 테스트
    @Test
    @DisplayName("댓글 생성 성공 : 로그인 유저가 특정 게시글에 댓글을 달면 201 Created 와 댓글 데이터가 반환")
    void createComment_success() throws Exception {
        // [GIVEN]
        // 1) 테스트용 유저 엔티티 생성
        User user = User.builder()
                .username("commentUser1")
                .password("Password123!")
                .email("test@example.com")
                .nickname("댓글작성자")
                .build();
        user = userRepository.save(user);// DB 에 저장 후, 영속 상태 엔티티로 다시 받음

        // 2) 테스트용 게시글 엔티티 생성
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .author(user)
                .displayNumber(1L)
                .build();
        post = postRepository.save(post); // DB 에 게시글 저장

        // 3) 댓글 생성 요청 DTO 생성 (클라이언트에서 보낼 내용 흉내)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("첫 번째 댓글")
                .build();

        // 4) DTO 를 JSON 문자열로 직렬화
        String requestBody = objectMapper.writeValueAsString(requestDto); // CommentCreateRequestDto -> JSON

        // 5) @AuthenticationPrincipal(expression = "id") 에서 사용할 테스트용 UserDetails 생성
        TestUserDetails principal = new TestUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")) // 권한 목록 (ROLE_USER 하나 부여)
        );

        // [WHEN] API 호출

        mockMvc.perform(
                        post("/api/posts/{postId}/comments", post.getId()) // POST /api/posts/{postId}/comments
                                .with(user(principal)) // 로그인 유저 정보를 Security 컨텍스트에 심어줌
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                ).andDo(print())

                // [THEN] JSON 검증
                .andExpect(status().isCreated()) // HTTP 상태 코드가 201 Created 인지 확인
                .andExpect(jsonPath("$.success").value(true))
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
        User user = userRepository.save(
                User.builder()
                        .username("commentUser1")
                        .password("Password123!")
                        .nickname("닉네임1")
                        .email("login@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("제목")
                        .content("내용")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );

        // 요청 DTO (Comment)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("비로그인 댓글")
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);

        // [WHEN & THEN]
        mockMvc.perform(
                post("/api/posts/{postId}/comments", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
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
        User user = userRepository.save(
                User.builder()
                        .username("commentUser1")
                        .password("Password123!")
                        .nickname("닉네임1")
                        .email("blank@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );

        //로그인 유저 principal 생성(인증)
        TestUserDetails principal = new TestUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 요청 DTO ( 댓글 내용 공백 )
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content(" ")
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);

        // [WHEN & THEN] 유효성 검증 실패 → 400 Bad Request 기대
        mockMvc.perform(
                        post("/api/posts/{postId}/comments", post.getId())
                                .with(user(principal)) // 로그인한 사용자로 요청
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andDo(print())
                .andExpect(status().isBadRequest()); // DTO 검증 실패 >> 400 응답 기대
    }



    // ⭐ 댓글 목록 조회 성공 테스트
    @Test
    @DisplayName("댓글 목록 조회 성공 : 특정 게시글 댓글 목록을 200 OK로 반환")
    void getCommentsByPost_success() throws Exception {
        // [GIVEN] 유저, 게시글, 댓글 2개 생성
        User user = userRepository.save(
                User.builder()
                        .username("commentUser1")
                        .password("Password123!")
                        .nickname("닉네임1")
                        .email("comment@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment1 = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("첫 번째")
                        .build()
        );
        Comment comment2 = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("두 번째")
                        .build()
        );

        // [WHEN & THEN] GET api/posts/{postId}/comments" 호출
        mockMvc.perform(
                get("/api/posts/{postId}/comments", post.getId())
        )
                .andDo(print())
                .andExpect(status().isOk()) // HTTP 200
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2)) //목록 크기 검증
                .andExpect(jsonPath("$.data[0].content").value("두 번째"))// DESC정렬
                .andExpect(jsonPath("$.data[1].content").value("첫 번째"))// DESC정렬
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
                        .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isNotFound()) // 404 NOT FOUND
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message")
                        .value("게시글을 찾을 수 없습니다. id=" + invalidPostId));
    }



    // ⭐ 댓글 수정 성공 테스트 (작성자 본인 댓글 내용을 수정)
    @Test
    @DisplayName("댓글 수정 성공 : 작성자가 자신의 댓글 내용을 수정, 200 OK와 수정된 댓글 데이터가 반환")
    void updateComment_success() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 1개 생성
        User user = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname")
                        .email("email@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("수정 전")
                        .build()
        );

        // 로그인 유저 principal (작성자 본인)
        TestUserDetails principal = new TestUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                // ↑ SecurityContext에 저장될 사용자 권한 목록 ROLE_USER 부여 >> 인증된 사용자 요청 보냄
        );

        // 수정 요청 DTO (댓글 내용만 바뀐다고 가정)
        CommentUpdateRequestDto requestDto = CommentUpdateRequestDto.builder()
                .content("수정 후")
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);

        // [WHEN & THEN] 댓글 수정 API 호출
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal)) //작성자 본인 요청
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(comment.getId().intValue()))
                // ↑ 댓글 id가 DB에 저장된 comment id와 동일한지 검증
                .andExpect(jsonPath("$.data.postId").value(post.getId().intValue()))
                // ↑  응답 JSON postId 가 올바르게 매핑되었는지 확인
                .andExpect(jsonPath("$.data.authorId").value(user.getId().intValue()))
                // ↑  작성자 authorId 가 올바르게 매핑되었는지 확인
                .andExpect(jsonPath("$.data.content").value("수정 후"));

        // [THEN] DB 검증
        Comment updatedComment = commentRepository.findById(comment.getId())
                .orElseThrow(() -> new IllegalStateException("댓글이 DB에 존재하지 않습니다."));

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
        User user = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname")
                        .email("email@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("수정 전")
                        .build()
        );

        // 비로그인 상태에서 보낼 수정 요청 본문 (JSON)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("비로그인 수정 시도")
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);

        // [WHEN & THEN] 로그인 없이 PATCH 요청 >> 401 기대
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())// 댓글 수정 URL
                        .contentType(MediaType.APPLICATION_JSON)
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
        User user1 = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname1")
                        .email("email1@example.com")
                        .build()
        );
        User user2 = userRepository.save(
                User.builder()
                        .username("username2")
                        .password("Password123!")
                        .nickname("nickname2")
                        .email("email2@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user1)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user1)
                        .content("수정 전")
                        .build()
        );

        // 다른 사용자로 로그인한 것처럼 Principal 생성
        TestUserDetails principal = new TestUserDetails(
                user2.getId(),
                user2.getUsername(),
                user2.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")) //권한 설정, 작성자가 아닌 일반 사용자
        );

        // 수정 요청 DTO (내용 변경)
        CommentCreateRequestDto requestDto = CommentCreateRequestDto.builder()
                .content("수정 시도")
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);

        // [WHEN & THEN] user2 가 user1의 댓글 수정 API 호출 >> 400 기대
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal)) // user2
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andDo(print())
                .andExpect(status().isBadRequest()) //400 Bad Request
                .andExpect(jsonPath("$.success").value(false))// ApiResponse.success = false
                .andExpect(jsonPath("$.data.message")
                        .value("댓글 작성자만 댓글을 수정할 수 있습니다."));
    }



    // ⭐ 댓글 수정 실패 테스트
    @Test
    @DisplayName("댓글 수정 실패 : 내용이 공백이면 400 Bad Request가 발생")
    void updateComment_fail_blankContent() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname")
                        .email("email@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("수정 전")
                        .build()
        );

        // 로그인 유저 principal
        TestUserDetails principal = new TestUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 요청 DTO
        CommentUpdateRequestDto requestDto = CommentUpdateRequestDto.builder()
                .content(" ")
                .build();
        String requestBody = objectMapper.writeValueAsString(requestDto);

        // [WHEN & THEN] 공백 내용으로 수정 요청
        mockMvc.perform(
                patch("/api/comments/{commentId}", comment.getId())
                        .with(user(principal)) // 로그인 작성자 본인
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        )
                .andDo(print())
                .andExpect(status().isBadRequest()) // DTO @NotBlank 검증 실패
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글 내용은 공백일 수 없습니다."));
    }




    // ⭐댓글 삭제 성공 테스트
    @Test
    @DisplayName("댓글 삭제 성공 : 작성자가 자신의 댓글을 삭제하면 200 OK와 성공 메시지가 반환")
    void deleteComment_success() throws Exception{
        // [GIVEN] 유저, 게시글, 댓글 생성
        User user = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname")
                        .email("email@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("삭제 전")
                        .build()
        );

        // 로그인 유저 principal
        TestUserDetails principal = new TestUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        Long commentId = comment.getId(); // 삭제 대상 댓글ID

        // [WHEN & THEN] 댓글 삭제 API 호출
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .with(user(principal)) // 로그인(작성자 본인) 정보 주입
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
        User user = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname")
                        .email("email@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user)
                        .content("삭제 전")
                        .build()
        );
        Long commentId = comment.getId(); // 삭제 시도할 댓글 ID

        // [WHEN & THEN] 로그인 정보 없이 DELETE 요청 → 401 기대
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
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
        User user1 = userRepository.save(
                User.builder()
                        .username("username1")
                        .password("Password123!")
                        .nickname("nickname1")
                        .email("email1@example.com")
                        .build()
        );
        User user2 = userRepository.save(
                User.builder()
                        .username("username2")
                        .password("Password123!")
                        .nickname("nickname2")
                        .email("email2@example.com")
                        .build()
        );
        Post post = postRepository.save(
                Post.builder()
                        .title("title")
                        .content("content")
                        .author(user1)
                        .displayNumber(1L)
                        .build()
        );
        Comment comment = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .author(user1)
                        .content("삭제 전")
                        .build()
        );
        Long commentId = comment.getId(); //삭제 시도할 commentID

        // user2 로 로그인 설정
        TestUserDetails principal = new TestUserDetails(
                user2.getId(),
                user2.getUsername(),
                user2.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))// 권한 설정
        );

        // [WHEN & THEN] user2 계정으로 댓글 삭제 요청
        mockMvc.perform(
                delete("/api/comments/{commentId}", commentId)
                        .with(user(principal))
        )
                .andDo(print())
                .andExpect(status().isBadRequest())// IllegalArgumentException  400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글 작성자만 삭제할 수 있습니다."));

        // [THEN] DB 검증 - 댓글은 여전히 존재해야 함 (삭제되면 안 됨)
        boolean exists = commentRepository.findById(commentId).isPresent();
        assertThat(exists).isTrue();
    }




    // ⭐ 댓글 삭제 실패 테스트
    @Test
    @DisplayName("댓글 삭제 실패 : 존재하지 않는 댓글 ID 삭제 시 400 Bad Request 반환")
    void deleteComment_fail_notFoundComment() throws Exception {
        // [GIVEN] 유저 1명 생성 (로그인용)
        User user = userRepository.save(
                User.builder()
                        .username("testUser1")
                        .password("Password123!")
                        .nickname("닉네임1")
                        .email("test@example.com")
                        .build()
        );

        // 로그인 principal 생성
        TestUserDetails principal = new TestUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        // 존재하지 않는 댓글 ID 지정
        Long invalidCommentId = 9999999L;

        // [WHEN & THEN] 존재하지 않는 댓글 삭제 시도 → 400 Bad Request
        mockMvc.perform(
                        delete("/api/comments/{commentId}", invalidCommentId)
                                .with(user(principal)) // 로그인한 사용자로 요청
                )
                .andDo(print())
                .andExpect(status().isBadRequest())   // IllegalArgumentException 400
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.message")
                        .value("댓글을 찾을 수 없습니다. id=" + invalidCommentId));
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
}
