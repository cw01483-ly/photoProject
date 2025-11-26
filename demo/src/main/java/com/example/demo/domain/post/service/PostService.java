package com.example.demo.domain.post.service;

import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.post.dto.PostDetailResponseDto;
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

import java.util.List;

@Service
@RequiredArgsConstructor // final 필드를 매개변수로 받는 생성자 자동생성
@Transactional(readOnly = true) // 이 클래스 모든 메서드는 읽기 전용
/*
    PostService
        1 게시글 생성
        2 단건 조회 + 조회수 증가 + 게시글 상세 조회 ( 게시글 + 댓글 목록 동시 조회 ) 메서드
        3 전체 조회 (페이징)
        4 작성자 기준 조회
        5 검색 (제목 + 내용)
        6 게시글 수정 (Update)
        7 게시글 삭제 (Soft Delete)
*/
public class PostService {
    private final PostRepository postRepository;
    // 게시글과 관련된 DB 작업을 담당하는 리포지토리
    private final UserRepository userRepository;
    // 작성자 조회하기 위한 리포지토리
    private final CommentRepository commentRepository;
    // 댓글 조회를 위한 리포지토리 ( 게시글 상세 조회 시, 해당 게시글의 댓글 목록 가져오기 위해)

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

    // 게시글 상세 조회 ( 게시글 + 댓글 목록 동시 조회 )
    //      - 단건 게시글 정보 + 조회수 증가 + 해당 게시글 달린 댓글 목록 반환
    @Transactional //조회수 증가 + 댓글 조회 포함 > 데이터 변경
    public PostDetailResponseDto getPostDetail(Long postId){
        // 1) 조회수 증가 (게시글 없으면 예외 발생)
        int updated = postRepository.increaseViews(postId);
        if (updated == 0){
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" +postId);
        }
        // 2)실제 게시글 엔티티 조회 ( softDelete 적용으로 삭제된 글 자동제외 )
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
        /* 3) 해당 게시글의 댓글 엔티티 목록 조회
            -CommentRepository.findByPostId(postId) 사용
            -Comment 엔티티에 @Where 적용으로 논리 삭제된 댓글은 자동 제외
        */
        List<Comment> comments = commentRepository.findByPostId(postId);

        /* 4) 댓글 엔티티 목록 -> 댓글 응답 DTO 목록으로 변환
            - CommentResponseDto.from(comment)을 각 댓글에 적용하여
              화면에 내려줄 형태(List<CommentResponseDto>)로 변환
         */
        List<CommentResponseDto> commentDtoList = comments.stream()
                .map(CommentResponseDto::from)
                .toList();
        /* 5) PostDetailResponseDto 로 통합 응답 생성
         - Post 엔티티 + 댓글 DTO 목록을 하나의 응답 객체로 묶어서 반환
        */
        return PostDetailResponseDto.from(post, commentDtoList);
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

    // 6. 게시글 수정 -> 수정을 위해 별도 트랜잭션 필요
    @Transactional
    /* 6-1) 수정 게시글 조회
        - 게시글 없으면 예외
        - softDelete 적용으로 삭제된 글 조회 X
    */
    public PostResponseDto updatePost(Long postId, String title, String content){

        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        /* 6-2) 엔티티의 비지니스 메서드 사용하여 제목/내용 수정
            - Post.update(title,content) 내부에서 null,공백 체크 + trim 처리
            - JPA 변경 감지(Dirty Checking)에 의해 트랜잭션 종료시 자동으로 UPDATE 쿼리 실행
        */
        post.update(title,content);

        // 6-3) 수정된 엔티티를 DTO 변환 후 반환
        return PostResponseDto.from(post);
    }

    // 7. 게시글 삭제 ( Soft Delete )
    @Transactional
    public void deletePost(Long postId){
        /* 7-1) 삭제할 게시글 조회
            - softDelete 적용(@Where is_deleted=false)
            - 이미 삭제된 글(is_deleted=true)은 조회 X
        */
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id="+postId));

        /* 7-2) 엔티티의 delete() 메서드 호출
            - Post.delete() 내부에서 isDeleted=true로 설정
            - @Where(is_deleted=false)에 의해 이후 조회에서 자동 제외
            - @SQLDelete와 함께 사용할 경우, 실제 delete(post) 호출과
              전략을 통일하는 방식도 있으나
              여기서는 엔티티 비지니스 메서드를 통한 SoftDelete 패턴 사용
        */

        post.delete();
        // JPA 변경 감지에 의해 트랜잭션 종료시 UPDATE쿼리 실행
        // ->> (is_deleted = true)로 변경
    }

}
