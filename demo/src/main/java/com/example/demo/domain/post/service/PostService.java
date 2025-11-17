package com.example.demo.domain.post.service;

import com.example.demo.domain.post.dto.PostResponseDto;
import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final 필드를 매개변수로 받는 생성자 자동생성
@Transactional(readOnly = true) // 이 클래스 모든 메서드는 읽기 전용
/*
    PostService
        1 게시글 생성
        2 단건 조회 + 조회수 증가
        3 전체 조회 (페이징)
        4 작성자 기준 조회
        5 검색 (제목 + 내용)
*/
public class PostService {
    private final PostRepository postRepository;
    // 게시글과 관련된 DB 작업을 담당하는 리포지토리
    private final UserRepository userRepository;
    // 작성자 조회하기 위한 리포지토리

    // 1. 게시글 생성
    @Transactional // 글생성은 데이터 변경, readOnly=false 트랜잭션 실행
    public PostResponseDto createPost(Long authorId, String title, String content){
        // 게시글 작성자 확인(회원만 글 작성 허용)
        User author = userRepository.findById(authorId)
                .orElseThrow(
                        ()->new IllegalArgumentException("작성자를 찾을 수 없습니다. id=" + authorId));
                // authorId로 DB검색, 없으면 예외 던지기

        // displayNumber 가장 큰 수 DB에서 찾기, softDelete 자동 제외
        Long maxNumber = postRepository.findMaxDisplayNumber();

        // 추가될 게시글 번호 계산,생성
        Long nextNumber = maxNumber+1;

        // Builder사용 게시글 생성
        Post post = Post.builder()
                .title(title)
                .content(content)
                .author(author)
                .displayNumber(nextNumber)
                .build();

        // DB에 저장 -> 저장된 엔티티 반환
        Post saved = postRepository.save(post);

        // 엔티티를 DTO로 변환 , Controller에 반환
        return PostResponseDto.from(saved);
    }

    // 2. 게시글 단건 조회 + 조회수 증가
    @Transactional //조회수 증가 때문에 데이터 변경 필요
    public PostResponseDto getPostById(Long postId){

        /* 2-1) 조회수 증가
            - 벌크 업데이트 -> 엔티티 변화 감지와 관계 없이 DB에서 바로 증가
            - updated가 0이면 해당 ID의 글 자체가 없음 -> 예외처리
         */
        int updated = postRepository.increaseViews(postId);

        if (updated == 0){
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id="+postId );
        }

        /* 2-2) 실제 게시글 조회
            - softDelete 적용(@Where is_deleted=false)
            - 삭제된 글은 자동으로 조회 불가
        */
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id="+postId));

        // 2-3) DTO로 반환하여 반환
        return PostResponseDto.from(post);
    }

    // 3. 최신 게시글 전체 조회 (페이징)
    public Page<PostResponseDto> getPosts(Pageable pageable){

        /* 3-1) 게시글 목록 조회
             - Post_id 기준 내림차순(최신글이 위로)
             - Pageable을 통해 page, size, sort 지정 가능
        */
        Page<Post> posts = postRepository.findByOrderByIdDesc(pageable);

        // 3-2) Page<Post> -> Page<PostResponseDto> 변환해서 반환
        return posts.map(PostResponseDto::from);
    }

    // 4. 작성자 기준 게시글 조회 (페이징)
    public Page<PostResponseDto> getPostsByAuthor(Long authorId, Pageable pageable){

        // 4-1) 작성자 ID(authorId) 기준 게시글 목록 페이징 조회
        Page<Post> posts = postRepository.findByAuthorId(authorId,pageable);

        // 4-2) DTO로 변환 후 반환
        return posts.map(PostResponseDto::from);
    }

    // 5. 제목 + 내용 키워드 검색 (IgnoreCase, 페이징)
    public Page<PostResponseDto> searchPosts(String keyword, Pageable pageable){

        // 5-1) keyword가 제목or내용에 포함된 글 검색
        Page<Post> posts = postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                        keyword, keyword, pageable
                );
        // 5-2) DTO로 변환 후 반환
        return posts.map(PostResponseDto::from);
        /*
            PostResponseDto::from
                        ==
            “posts(Page<Post>) 안에 들어 있는 모든 Post를
            PostResponseDto.from(Post)로 변환해서
            Page<PostResponseDto> 로 만들어서 반환해라.”*/
    }

}
