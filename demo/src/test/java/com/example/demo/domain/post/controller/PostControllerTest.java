package com.example.demo.domain.post.controller;

import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.repository.PostRepository; // 게시글 DB 검증용
import com.example.demo.domain.user.entity.User;              // 작성자 엔티티
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.global.security.jwt.properties.JwtProperties;
import com.example.demo.support.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import static org.assertj.core.api.Assertions.assertThat; // DB 검증용
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print; // 결과 콘솔 출력
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath; // JSON 응답 검증용
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status; // HTTP 상태코드 검증용

/*
    PostControllerTest

    - 실제 HTTP 요청/응답 흐름을 MockMvc를 사용해 테스트하는 클래스
    - PostController의 REST API가 URL, HTTP 메서드, 요청/응답 형식에 맞게 동작하는지 검증
    - 서비스 로직(PostService) 자체는 별도 단위 테스트로 검증할 수 있지만,
      여기서는 "웹 레이어(컨트롤러 + JSON/파라미터 매핑 + 예외 핸들링)"을 집중적으로 확인
 */

@ActiveProfiles("test")
public class PostControllerTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/posts";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MockMvc mockMvc;
    /*
        MockMvc
        - 실제 서버를 띄우지 않고도 스프링 MVC 동작을 테스트하게 해주는 도구
        - URL, HTTP 메서드, 파라미터 등을 지정해서 요청을 보내고,
          응답 코드(status)나 응답 JSON 내용을 검증할 수 있음
     */

    @Autowired
    private ObjectMapper objectMapper;
    /*
        ObjectMapper
        - 자바 객체 <-> JSON 문자열 변환기
     */

    @Autowired
    private UserRepository userRepository;
    /*
        UserRepository
        - 게시글 작성 시 필요한 "작성자(User)"를 미리 DB에 저장하기 위한 용도
        - PostService.createPost(authorId, ...) 가 내부에서 작성자를 조회하므로,
          테스트가 실행되기 전에 해당 authorId를 가진 User가 DB에 존재해야 함
     */

    @Autowired
    private PostRepository postRepository;
    /*
        PostRepository
        - PostController를 통해 게시글 생성/삭제가 된 후,
          실제 DB에 반영되었는지 2차 검증을 하기 위한 용도
     */

    // ⭐ 게시글 생성 성공 테스트 (POST /api/posts)
    @Test
    @DisplayName("게시글 생성 성공 : POST /api/posts 호출 시 200과 PostResponseDto 반환 ")
    void createPost_success() throws Exception{
        // [GIVEN-1] 게시글 작성자 생성
        String rawPw = "Password123!";

        User author = userRepository.save(
                User.builder()
                        .username("postauthor1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("게시글작성자")
                        .email("author@example.com")
                        .build()
        );

        // [GIVEN-2] 로그인 요청 바디(JSON) 준비
        String loginJson = """
                {
                    "username": "postauthor1",
                    "password": "Password123!"
                }
                """;

        // [WHEN-1] 로그인 후 JWT 쿠키 발급받기
        var loginResult = mockMvc.perform(
                post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // [THEN-1]  로그인 응답에서 JWT 쿠키 추출
        Cookie jwtCookie = loginResult.getResponse().getCookie(jwtProperties.getCookieName());
        assertThat(jwtCookie).isNotNull(); //쿠키 null이면 실패

        // [GIVEN-3] 게시글 생성 요청 JSON 바디 준비
        String postCreateJson = """
                {
                    "title": "제목",
                    "content": "내용"
                }
                """;

        // [WHEN-2] JWT 쿠키를 포함하여 POST /api/posts 요청 전송
        var resultAction = mockMvc.perform(
                post(BASE_URL)
                        .cookie(jwtCookie)// JwtAuthenticationFilter가 쿠키에서 JWT를 읽어 SecurityContext에 인증을 올림
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postCreateJson)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-1] HTTP 응답 코드 및 공통 응답 포맷 구조 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 생성 완료"))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("내용"))
                .andExpect(jsonPath("$.data.authorName").value("게시글작성자"));// 닉네임

        // [THEN-2] DB에 게시글이 존재하는지 2차 검증
        String responseBody = resultAction.andReturn().getResponse().getContentAsString();
        // ↑ 응답 JSON 전체를 문자열로 가져오기
        long createdPostId = objectMapper.readTree(responseBody).path("data").path("id").asLong();
        // ↑ 그 JSON에서 data.id를 찾아 long 값으로 꺼내기
        assertThat(createdPostId).isGreaterThan(0L); // 생성된 id가 0보다 큰지 확인
        assertThat(postRepository.existsById(createdPostId)).isTrue(); // 실제 DB에 존재하는지 확인
    }



    // ⭐ 게시글 단건 조회 성공 테스트 (GET /api/posts/{id})
    @Test
    @DisplayName("게시글 단건 조회 성공 : GET /api/posts/{id} 호출 시 200과 PostResponseDto 반환")
    void getPostById_success() throws Exception{
        // [GIVEN-1] 게시글 작성자 생성
        String rawPw = "Password123!";
        User author = userRepository.save(
                User.builder()
                        .username("postauthor1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("조회작성자")
                        .email("author@example.com")
                        .build()
        );

        // [GIVEN-2] 게시글 저장 (처음 조회수 0)
        Post savedPost = postRepository.save(
                Post.builder()
                        .title("테스트제목")
                        .content("테스트 내용")
                        .author(author)
                        .displayNumber(1L)// displayNumber는 내부 정렬용 번호라 임의 값 사용 OK
                        .build()
        );
        Long postId = savedPost.getId(); // GET 요청할 게시글 ID

        // [WHEN] GET /api/posts/{id} 요청 보내기
        var resultAction = mockMvc.perform(
                get(BASE_URL+"/{postId}", postId)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] 응답코드 + ApiResponse 구조 + 반환 DTO 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 단건 조회 성공"))
                .andExpect(jsonPath("$.data.id").value(postId.intValue()))
                .andExpect(jsonPath("$.data.title").value("테스트제목"))
                .andExpect(jsonPath("$.data.content").value("테스트 내용"))
                .andExpect(jsonPath("$.data.authorName").value("조회작성자"))
                .andExpect(jsonPath("$.data.views").value(1))
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }


    // ⭐ 전체 게시글 목록 조회 성공 테스트 (GET /api/posts)
    @Test
    @DisplayName("전체 조회 성공 : GET /api/posts 호출 시 200, 페이징된 게시글 목록 반환")
    void getPosts_success() throws Exception{
        // [GIVEN-1] 게시글 작성자 생성
        String rawPw = "Password123!";
        User author = userRepository.save(
                User.builder()
                        .username("postauthor1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("목록작성자")
                        .email("list@example.com")
                        .build()
        );

        // [GIVEN-2] 게시글 여러개 저장
        Post post1 = postRepository.save(
                Post.builder()
                        .title("제목1")
                        .content("내용1")
                        .author(author)
                        .displayNumber(1L) // 정렬용 번호(임의)
                        .build()
        );

        Post post2 = postRepository.save(
                Post.builder()
                        .title("제목2")
                        .content("내용2")
                        .author(author)
                        .displayNumber(2L) // 정렬용 번호(임의)
                        .build()
        );

        // [WHEN] GET /api/posts?page=0&size=10 요청 보내기
        var resultAction = mockMvc.perform(
                get(BASE_URL)
                        .param("page", "0") // 0번 페이지
                        .param("size", "10") // 페이지당 게시글 수
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-1] HTTP 상태 코드 및 ApiResponse 기본 구조 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray());// data.content가 배열인지 확인

        // [THEN-2] 게시글 목록안에 제목 확인
        resultAction
                .andExpect(jsonPath("$.data.content[?(@.title == '제목1')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.title == '제목2')]").exists());
    }


    // ⭐ 작성자별 게시글 목록 조회 성공 테스트 (GET /api/posts/author/{authorId})
    @Test
    @DisplayName("작성자별 조회 성공 : GET /api/posts/author/{authorId} 호출 시 해당 작성자의 게시글만 페이징해서 반환")
    void getPostsByAuthor_success() throws Exception{
        // [GIVEN-1] 게시글 작성자 2명 생성
        String rawPw = "Password123!";
        User author1 = userRepository.save(
                User.builder()
                        .username("author1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("작성자1")
                        .email("user1@example.com")
                        .build()
        );
        User author2 = userRepository.save(
                User.builder()
                        .username("author2")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("작성자2")
                        .email("user2@example.com")
                        .build()
        );

        // [GIVEN-2] author1의 게시글 2개 생성
        Post author1Post1 = postRepository.save(
                Post.builder()
                        .title("제목1")
                        .content("내용1")
                        .author(author1)
                        .displayNumber(1L)
                        .build()
        );
        Post author1Post2 = postRepository.save(
                Post.builder()
                        .title("제목2")
                        .content("내용2")
                        .author(author1)
                        .displayNumber(2L)
                        .build()
        );

        // [GIVEN-3] author2의 게시글 1개생성
        Post author2Post1 = postRepository.save(
                Post.builder()
                        .title("제목3")
                        .content("내용3")
                        .author(author2)
                        .displayNumber(3L)
                        .build()
        );
        Long authorId = author1.getId(); // 조회 대상 작성자 ID

        // [WHEN] GET /api/posts/author/{authorId}?page=0&size=10 요청 보내기
        var resultAction = mockMvc.perform(
                get(BASE_URL+"/author/{authorId}", authorId)
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-1] HTTP 상태 코드 및 기본 응답 구조 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("작성자별 게시글 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray());

        // [THEN-2]  data.content 안에 "작성자1"의 글들은 포함되고, 작성자2는 제외되었는지 검증
        resultAction
                .andExpect(jsonPath("$.data.content[?(@.title == '제목1')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.title == '제목2')]").exists())
                // ↓ 포함되면 안되므로 doesNotExist
                .andExpect(jsonPath("$.data.content[?(@.title == '제목3')]").doesNotExist());
    }



    // ⭐ 게시글 검색 성공 테스트 (GET /api/posts/search)
    @Test
    @DisplayName("검색 성공 : keyword가 제목&내용 포함된 게시글만 페이징 후 반환")
    void searchPosts_success() throws Exception{
        // [GIVEN-1] 작성자 생성
        String rawPw = "Password123!";
        User author = userRepository.save(
                User.builder()
                        .username("author1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("작성자1")
                        .email("example@example.com")
                        .build()
        );

        // [GIVEN-2] 검색 키워드가 포함된 게시글 저장(keyword : 테스트)
        Post testPost1 = postRepository.save(
                Post.builder()
                        .title("테스트 제목1")
                        .content("테스트 내용1")
                        .author(author)
                        .displayNumber(1L)
                        .build()
        );
        Post testPost2 = postRepository.save(
                Post.builder()
                        .title("제목2")
                        .content("테스트 내용2")
                        .author(author)
                        .displayNumber(2L)
                        .build()
        );

        // [GIVEN-3] 키워드 미포함 게시글 생성
        Post testPost3 = postRepository.save(
                Post.builder()
                        .title("제목")
                        .content("내용")
                        .author(author)
                        .displayNumber(3L)
                        .build()
        );

        String keyword = "테스트"; // 검색어

        // [WHEN] GET /api/posts/search?keyword=테스트&page=0&size=10 요청
        var resultAction = mockMvc.perform(
                get(BASE_URL+"/search")
                        .param("keyword", keyword) // 검색어 파라미터
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-1] HTTP 상태 코드 및 기본 응답 구조 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 검색 성공"))
                .andExpect(jsonPath("$.data.content").isArray());

        // [THEN-2] '테스트' 포함된 게시글 존재 O, '테스트'와 무관한 게시글은 존재 X
        resultAction
                // 제목or내용에 '테스트' 확인
                .andExpect(jsonPath("$.data.content[?(@.title == '테스트 제목1')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.title == '제목2')]").exists())
                // 검색어와 무관한 글은 없어야함
                .andExpect(jsonPath("$.data.content[?(@.title == '제목')]").doesNotExist());
    }


    // ⭐ 게시글 수정 성공 테스트 (PUT /api/posts/{id})
    @Test
    @DisplayName("게시글 수정 성공 : 유효한 값으로 PUT /api/posts/{id} 호출 시 200과 수정된 PostResponseDto 반환")
    void updatePost_success() throws Exception{
        // [GIVEN-1] 게시글 작성자 생성
        String rawPwd = "Password123!";
        User author = userRepository.save(
                User.builder()
                        .username("author1")
                        .password(passwordEncoder.encode(rawPwd))
                        .nickname("수정작성자")
                        .email("update@example.com")
                        .build()
        );

        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
            {
                "username": "author1",
                "password": "Password123!"
            }
            """;

        // [WHEN-1] 로그인 → JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());

        assertThat(jwtCookie).isNotNull();

        // [GIVEN-3] 업데이트 대상 개시글 생성
        Post post1 = postRepository.save(
                Post.builder()
                        .title("원래 제목")
                        .content("원래 내용")
                        .displayNumber(1L)
                        .author(author)
                        .build()
        );
        Long postId = post1.getId();

        // [GIVEN-4] 수정할 제목 & 내용 준비
        String newTitle = "새로운 제목";
        String newContent = "새로운 내용";

        // [WHEN-2] PUT /api/posts/{id}?title=...&content=... 요청 전송
        var resultAction = mockMvc.perform(
                put(BASE_URL + "/{postId}", postId)
                        .cookie(jwtCookie) // JWT인증 추가
                        .param("title", newTitle)
                        .param("content", newContent)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-1] 응답 코드 및 ApiResponse 구조 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 수정 성공"))
                // 수정된 제목, 내용 이 응답에 반영되었는지 확인
                .andExpect(jsonPath("$.data.id").value(postId.intValue()))
                .andExpect(jsonPath("$.data.title").value(newTitle))
                .andExpect(jsonPath("$.data.content").value(newContent))
                .andExpect(jsonPath("$.data.authorName").value("수정작성자"));
                // ↑ 작성자 닉네임 유지 확인

        // [THEN-2] DB에서 제목 내용 수정 2차 검증
        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertThat(updatedPost.getTitle()).isEqualTo(newTitle);
        assertThat(updatedPost.getContent()).isEqualTo(newContent);
    }




    // ⭐ 게시글 삭제 성공 테스트 (DELETE /api/posts/{id})
    @Test
    @DisplayName("게시글 삭제 성공 : DELETE /api/posts/{id} 호출 시 200, 완료 메시지 반환")
    void deletePost_success() throws Exception{
        // [GIVEN-1] 작성자 생성
        String rawPwd = "Password123!";
        User author = userRepository.save(
                User.builder()
                        .username("author1")
                        .password(passwordEncoder.encode(rawPwd))
                        .nickname("삭제작성자")
                        .email("delete@example.com")
                        .build()
        );

        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
            {
                "username": "author1",
                "password": "Password123!"
            }
            """;

        // [WHEN-1] 로그인 → JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());

        assertThat(jwtCookie).isNotNull();


        // [GIVEN-3] 삭제 대상 게시글 생성
        Post post1 = postRepository.save(
                Post.builder()
                        .title("제목")
                        .content("내용")
                        .author(author)
                        .displayNumber(1L)
                        .build()
        );
        Long postId = post1.getId(); // 삭제할 게시글 PK

        // [WHEN] DELETE /api/posts/{id} 요청 전송
        var resultAction = mockMvc.perform(
                delete(BASE_URL + "/{postId}", postId)
                        .cookie(jwtCookie) // JWT 인증 추가
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-1] 응답 코드 & ApiResponse 기본 구조 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 삭제 성공"));

        // [THEN-2] DB에서 해당 게시글 삭제 2차 검증
        boolean exists = postRepository.existsById(postId); //삭제 후 존재 확인
        assertThat(exists).isFalse();
    }


    // ⭐ 게시글 좋아요 토글 성공 테스트 (POST /api/posts/{postId}/likes)
    @Test
    @DisplayName("게시글 좋아요 토글 성공 : 처음 호출 시 liked=true, likeCount=1 반환")
    void togglePostLike_success() throws Exception{
        // [GIVEN-1] 사용자 생성
        String rawPw = "Password123!";
        User user = userRepository.save(
                User.builder()
                        .username("user1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("nickname")
                        .email("like@example.com")
                        .build()
        );

        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
                {
                    "username": "user1",
                    "password": "Password123!"
                }
                """;

        // [WHEN-1] 로그인 후 JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());
        assertThat(jwtCookie).isNotNull();

        // [GIVEN-3] 게시글 생성
        Post post = postRepository.save(
                Post.builder()
                        .title("제목")
                        .content("내용")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Long postId = post.getId();

        // [WHEN-2] JWT 쿠키 포함하여 좋아요 토글 요청
        var resultAction = mockMvc.perform(
                post(BASE_URL + "/{postId}/likes", postId)
                        .cookie(jwtCookie) // userId 파라미터 제거, JWT인증 사용
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] 응답 코드 + ApiResponse 구조 + DTO 필드 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 좋아요 토글 성공"))
                .andExpect(jsonPath("$.data.postId").value(postId.intValue()))
                .andExpect(jsonPath("$.data.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1)); // 첫토글 = likeCount 1
    }



    // ⭐ 게시글 좋아요 개수 조회 성공 테스트 (GET /api/posts/{postId}/likes/count)
    @Test
    @DisplayName("게시글 좋아요 개수 조회 성공 좋아요 1개인 게시글의 likeCount=1 반환")
    void getPostLikeCount_success() throws Exception{
        // [GIVEN-1] 사용자 생성
        String rawPw = "Password123!";

        User user = userRepository.save(
                User.builder()
                        .username("user1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("usernick")
                        .email("like@example.com")
                        .build()
        );

        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
            {
                "username": "user1",
                "password": "Password123!"
            }
            """;

        // [WHEN-1] 로그인 → JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());

        assertThat(jwtCookie).isNotNull(); // 쿠키 null이면 실패(이후 좋아요 요청이 401 될 수 있음)

        // [GIVEN-] 게시글 생성
        Post post = postRepository.save(
                Post.builder()
                        .title("제목")
                        .content("내용")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Long postId = post.getId();

        // [GIVEN-3] 게시글에 미리 LikeCount 추가
        mockMvc.perform(
                post(BASE_URL + "/{postId}/likes", postId)
                        .cookie(jwtCookie)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [WHEN] GET /api/posts/{postId}/likes/count 요청 전송 (likeCount 조회 요청)
        var resultAction = mockMvc.perform(
                get(BASE_URL + "/{postId}/likes/count", postId)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] 응답 코드 + ApiResponse 구조 + DTO 필드 검증
        resultAction
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 좋아요 개수 조회 성공"))
                .andExpect(jsonPath("$.data.postId").value(postId.intValue()))
                .andExpect(jsonPath("$.data.likeCount").value(1));//눌린 좋아요 수 출력
    }



    // ⭐ 게시글 좋아요 토글 2번 호출 시 최종 liked=false, likeCount=0 이어야 하는 테스트
    @Test
    @DisplayName("게시글 좋아요 토글 2회 호출 시 좋아요가 취소되고 likeCount=0 반환")
    void togglePostLike_twice_success() throws Exception{
        // [GIVEN-1] 사용자 생성
        String rawPw = "Password123!";
        User user = userRepository.save(
                User.builder()
                        .username("user1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("usernick")
                        .email("like@like.com")
                        .build()
        );

        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
            {
                "username": "user1",
                "password": "Password123!"
            }
            """;

        // [WHEN-1] 로그인 → JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());

        assertThat(jwtCookie).isNotNull(); // 쿠키가 없으면 이후 요청은 401 날 수 있음

        // [GIVEN-3] 게시글 생성
        Post post = postRepository.save(
                Post.builder()
                        .title("제목")
                        .content("내용")
                        .author(user)
                        .displayNumber(1L)
                        .build()
        );
        Long postId = post.getId();

        // [WHEN-2] 첫 번째 토글 호출 -> likeCount 증가
        var firstToggle = mockMvc.perform(
                post(BASE_URL + "/{postId}/likes", postId)
                        .cookie(jwtCookie) // userID파라미터 제거, JWT인증 사용
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());


        // [THEN-1] 첫 번째 요청 검증
        firstToggle
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 좋아요 토글 성공"))
                .andExpect(jsonPath("$.data.postId").value(postId.intValue())) // postId 일치
                .andExpect(jsonPath("$.data.userId").value(user.getId().intValue())) // userId 일치
                .andExpect(jsonPath("$.data.liked").value(true)) // 첫 토글 liked = true
                .andExpect(jsonPath("$.data.likeCount").value(1)); // likeCount 1

        // [WHEN-3] 두 번째 토글 호출 -> likeCount 감소
        var secondToggle = mockMvc.perform(
                post(BASE_URL + "/{postId}/likes", postId)
                        .cookie(jwtCookie) // ★ 동일 유저로 다시 호출
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN-2] 두 번째 요청 검증
        secondToggle
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 좋아요 토글 성공"))
                .andExpect(jsonPath("$.data.postId").value(postId.intValue()))
                .andExpect(jsonPath("$.data.userId").value(user.getId().intValue()))
                .andExpect(jsonPath("$.data.liked").value(false)) // 두 번째 토글 liked = false
                .andExpect(jsonPath("$.data.likeCount").value(0)); // likeCount 0
    }




    // ⭐ 게시글 단건 조회 실패 테스트 (GET /api/posts/{id} - 존재하지 않는 ID)
    @Test
    @DisplayName("게시글 단건 조회 실패 : 존재하지 않는 ID로 조회 시 에러 응답 반환")
    void getPostById_notFound() throws Exception{
        // [GIVEN] 존재하지 않는 ID 준비
        Long notExistingId = 9999999L;

        // [WHEN] GET /api/posts/{id} 요청 (존재 X)
        var resultAction = mockMvc.perform(
                get(BASE_URL + "/{postId}", notExistingId)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] 응답 코드, GlobalExceptionHandler + ApiResponse 구조 검증
        resultAction
                .andExpect(status().isBadRequest()) // IllegalArgumentException >> 400 Bad Request
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message") // 최상위 ApiResponse 필드 검증
                        .value("게시글을 찾을 수 없습니다. id="  + notExistingId))
                // ↓ data 속 ErrorResponse 구조 검증
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.status").value(400))
                .andExpect(jsonPath("$.data.message")
                        .value("게시글을 찾을 수 없습니다. id=" + notExistingId))
                .andExpect(jsonPath("$.data.path")
                        .value(BASE_URL + "/" + notExistingId));
    }



    // ⭐ 게시글 수정 실패 테스트 (PUT /api/posts/{id} - 존재하지 않는 ID)
    @Test
    @DisplayName("게시글 수정 실패 : 존재하지 않는 ID로 수정 요청 시 400과 에러 응답 반환")
    void updatePost_notFound() throws Exception {
        // [GIVEN-1] 로그인용 사용자 생성
        String rawPw = "Password123!";
        userRepository.save(
                User.builder()
                        .username("loginuser1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("로그인유저")
                        .email("loginuser@example.com")
                        .build()
        );
        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
            {
                "username": "loginuser1",
                "password": "Password123!"
            }
            """;
        // [WHEN-1] 로그인 → JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());

        assertThat(jwtCookie).isNotNull();

        // [GIVEN-3] 존재하지 않는 게시글 ID
        Long notExistingId = 999999L;

        // [WHEN-2] PUT /api/posts/{id}?title=...&content=... 요청 (없는 ID)
        var resultAction = mockMvc.perform(
                put(BASE_URL + "/{id}", notExistingId)
                        .cookie(jwtCookie)
                        .param("title", "제목")
                        .param("content", "내용")
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] GlobalExceptionHandler + ApiResponse + ErrorResponse 구조 검증
        resultAction
                // IllegalArgumentException → 400 Bad Request
                .andExpect(status().isBadRequest())

                // 최상위 ApiResponse 필드
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("게시글을 찾을 수 없습니다. id=" + notExistingId))

                // 내부 ErrorResponse 필드
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.status").value(400))
                .andExpect(jsonPath("$.data.message")
                        .value("게시글을 찾을 수 없습니다. id=" + notExistingId))
                .andExpect(jsonPath("$.data.path")
                        .value(BASE_URL + "/" + notExistingId));
        // timestamp는 매번 달라져서 검증하지 않음
    }



    // ⭐ 게시글 삭제 실패 테스트 (DELETE /api/posts/{id} - 존재하지 않는 ID)
    @Test
    @DisplayName("게시글 삭제 실패 : 존재하지 않는 게시글 삭제 요청 시 400과 에러 응답 반환")
    void deletePost_notFound() throws Exception {
        // [GIVEN-1] 로그인용 사용자 생성
        String rawPw = "Password123!";
        userRepository.save(
                User.builder()
                        .username("loginuser1")
                        .password(passwordEncoder.encode(rawPw))
                        .nickname("로그인유저")
                        .email("loginuser@example.com")
                        .build()
        );
        // [GIVEN-2] 로그인 요청(JSON)
        String loginJson = """
            {
                "username": "loginuser1",
                "password": "Password123!"
            }
            """;
        // [WHEN-1] 로그인 → JWT 쿠키 발급
        var loginResult = mockMvc.perform(
                        post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        Cookie jwtCookie = loginResult.getResponse()
                .getCookie(jwtProperties.getCookieName());

        assertThat(jwtCookie).isNotNull();


        // [GIVEN-3] 존재하지 않는 게시글 ID
        Long notExistingId = 999999L;

        // [WHEN-2] DELETE /api/posts/{id} 요청 (없는 ID)
        var resultAction = mockMvc.perform(
                delete(BASE_URL + "/{id}", notExistingId)
                        .cookie(jwtCookie)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(print());

        // [THEN] GlobalExceptionHandler + ApiResponse + ErrorResponse 구조 검증
        resultAction
                // IllegalArgumentException → 400 Bad Request
                .andExpect(status().isBadRequest())

                // 최상위 ApiResponse 필드
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("게시글을 찾을 수 없습니다. id=" + notExistingId))

                // 내부 ErrorResponse 필드
                .andExpect(jsonPath("$.data.success").value(false))
                .andExpect(jsonPath("$.data.status").value(400))
                .andExpect(jsonPath("$.data.message")
                        .value("게시글을 찾을 수 없습니다. id=" + notExistingId))
                .andExpect(jsonPath("$.data.path")
                        .value(BASE_URL + "/" + notExistingId));
    }


    // ⭐ 게시글 생성 실패 (비로그인)
    @Test
    @DisplayName("게시글 생성 실패 : 비로그인 사용자 POST /api/posts 호출 시 401 Unauthorized")
    void createPost_unauthorized_whenNotLogin() throws Exception {
        String postCreateJson = """
            {
                "title": "제목",
                "content": "내용"
            }
            """;

        mockMvc.perform(
                        post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(postCreateJson)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 401
    }



    // ⭐ 게시글 수정 실패 (비로그인)
    @Test
    @DisplayName("게시글 수정 실패 : 비로그인 사용자 PUT /api/posts/{id} 호출 시 401 Unauthorized")
    void updatePost_unauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(
                        put(BASE_URL + "/{postId}", 1L)
                                .param("title", "수정제목")
                                .param("content", "수정내용")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }



    // ⭐ 게시글 삭제 실패 (비로그인)
    @Test
    @DisplayName("게시글 삭제 실패 : 비로그인 사용자 DELETE /api/posts/{id} 호출 시 401 Unauthorized")
    void deletePost_unauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(
                        delete(BASE_URL + "/{postId}", 1L)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }


    // ⭐ 게시글 Like 실패 (비로그인)
    @Test
    @DisplayName("게시글 좋아요 토글 실패 : 비로그인 사용자가 POST /api/posts/{id}/likes 호출 시 401 Unauthorized")
    void togglePostLike_unauthorized_whenNotLogin() throws Exception {
        mockMvc.perform(
                        post(BASE_URL + "/{postId}/likes", 1L)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

}
