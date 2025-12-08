package com.example.demo.domain.post.controller;

import com.example.demo.domain.post.repository.PostRepository; // 게시글 DB 검증용
import com.example.demo.domain.user.entity.User;              // 작성자 엔티티
import com.example.demo.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat; // DB 검증용
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; // POST / 요청 생성 헬퍼
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
@SpringBootTest  // 스프링 부트 전체 컨텍스트 로드 (실제 앱과 거의 동일 환경)
@AutoConfigureMockMvc// MockMvc 자동 설정 (HTTP 요청/응답을 테스트 코드에서 시뮬레이션)
@Transactional // 각 테스트 후 DB 롤백 → 테스트 간 데이터 간섭 방지

public class PostControllerTest {

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
        - 현재 PostController는 @RequestParam 방식이라 JSON 바디는 쓰지 않지만,
          나중에 다른 테스트나 응답 본문 파싱 등에 사용할 수 있어 함께 주입
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

    // ⭐ 게시글 생성 성공 테스트 (POST /posts)
    @Test
    @DisplayName("게시글 생성 성공 : POST /posts 호출 시 200과 PostResponseDto 반환 ")
    void createPost_success() throws Exception{
        // [GIVEN-1] 게시글 작성자 생성
        User author = userRepository.save(
                User.builder()
                        .username("postauthor1")
                        .password("Password123!")
                        .nickname("게시글작성자")
                        .email("author@example.com")
                        .build()
        );

        // [GIVEN-2] 게시글 생성 시 사용할 제목과 내용
        String title = "테스트 게시글 제목";
        String content = "테스트 게시글 내용";

        // [WHEN] MockMvc 사용하여 POST /posts 요청 전송
        var resultAction = mockMvc.perform(
                post("/posts")
                        .param("authorId", author.getId().toString())
                        .param("title", title)
                        .param("content", content)
                        .accept(MediaType.APPLICATION_JSON)// JSON 응답 기대
        ).andDo(print());

        // [THEN-1] HTTP 응답 코드 및 공통 응답 포맷(ApiResponse) 구조 검증
        resultAction
                .andExpect(status().isOk()) // 200 Ok
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("게시글 생성 완료"))
                // data.title, data.content, data.authorName 등의 필드가 기대값과 일치하는지 확인
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.content").value(content))
                .andExpect(jsonPath("$.data.authorName").value("게시글작성자"));

        // [THEN-2] 실제 DB에도 게시글이 1건 이상 저장되었는지 2차 검증
        long postCount = postRepository.count(); // 현재 Post 테이블의 전체 개수
        assertThat(postCount).isGreaterThanOrEqualTo(1); // 적어도 1개 이상 존재
        // (주의: data.sql 등으로 초기 데이터가 있을 수 있으므로 "== 1" 보다는 >= 1로 검증)

    }
}
