package com.example.demo.domain.post.service;

import com.example.demo.domain.comment.dto.CommentResponseDto;
import com.example.demo.domain.comment.entity.Comment;
import com.example.demo.domain.comment.repository.CommentRepository;
import com.example.demo.domain.post.dto.PostDetailResponseDto;
import com.example.demo.domain.post.dto.PostResponseDto;
import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.repository.PostLikeRepository;
import com.example.demo.domain.post.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.global.file.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor // final 필드를 매개변수로 받는 생성자 자동생성
@Transactional(readOnly = true) // 이 클래스 모든 메서드는 읽기 전용
/*
    PostService
        1 게시글 생성
        2 단건 조회 + 조회수 증가 메서드
        3 게시글 상세 조회 (UI 상세 진입 전용)
        4 전체 조회 (페이징)
        5 작성자 기준 조회
        6 검색 (제목 + 내용)
        7 게시글 수정 (Update)
        8 게시글 삭제 (Soft Delete)
*/
public class PostService {
    private final PostRepository postRepository;
    // 게시글과 관련된 DB 작업을 담당하는 리포지토리
    private final UserRepository userRepository;
    // 작성자 조회하기 위한 리포지토리
    private final CommentRepository commentRepository;
    // 댓글 조회를 위한 리포지토리 ( 게시글 상세 조회 시, 해당 게시글의 댓글 목록 가져오기 위해)
    private final PostLikeRepository postLikeRepository;
    // 게시글 좋아요 정보를 조회하기 위한 리포지토리
    private final FileStorageService fileStorageService;
    // 이미지 저장/삭제 담당 서비스

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
        return PostResponseDto.from(saved, 0L); //새로만든 글은 좋아요 없다고 보고 likeCount 0으로 세팅
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

        // 2-3) 해당 게시글 좋아요 수 조회
        long likeCount = postLikeRepository.countByPostId(postId);

        // 2-4) DTO로 반환하여 반환 ( 조회수 + Like )
        return PostResponseDto.from(post, likeCount);
    }


    // 3. 게시글 상세 조회 (UI 상세 진입 전용)
    @Transactional
    public PostDetailResponseDto getPostDetailWithViewIncrease(Long postId){

        // 1) 조회수 증가 (게시글 없으면 예외)
        int updated = postRepository.increaseViews(postId);
        if (updated == 0){
            throw new IllegalArgumentException("게시글을 찾을 수  없습니다. id="+postId );
        }
            // 2) 증가 없는 상세 조회 로직 재사용
            return getPostDetail(postId); // getPostDetail이 조회수 증가를 하지 않으므로 1회만 증가
    }


    // 조회수 증가 없는 상세 데이터 조회 (수정폼/권한검증/내부조회/리다이렉트 후 재조회 등에 사용)
    public PostDetailResponseDto getPostDetail(Long postId){

        // 1)실제 게시글 엔티티 조회 ( softDelete 적용으로 삭제된 글 자동제외 )
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
        /* 2) 해당 게시글의 댓글 엔티티 목록 조회
                페이징을 사용하여 "최신 10개"만 조회
                + Page 객체를 통해 전체 댓글 개수도 함께 가져옴
            - Comment 엔티티에 @Where 적용으로 논리 삭제된 댓글은 자동 제외
        */

        //  한 번에 가져올 댓글 개수 설정 (최신 10개)
        int size = 10;

        //  0번 페이지(첫 페이지)를, id 기준 내림차순으로 조회하도록 Pageable 생성
        Pageable pageable = PageRequest.of(
                0,      // 첫 페이지(0부터 시작)
                size,              // 한 페이지당 10개
                Sort.by(Sort.Direction.DESC, "id")   // id 기준 내림차순 정렬
        );

        // 페이징으로 댓글 조회 (최신 10개 + 전체 개수 정보 포함)
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageable);

        // Page<Comment>에서 실제 데이터(댓글 10개까지)를 꺼냄
        List<Comment> latestComments = commentPage.getContent();

        // 전체 댓글 개수 (삭제되지 않은 댓글 기준)
        long totalCommentsCount = commentPage.getTotalElements();

        /* 3) 댓글 엔티티 목록 -> 댓글 응답 DTO 목록으로 변환
            - CommentResponseDto.from(comment)을 각 댓글에 적용하여
              화면에 내려줄 형태(List<CommentResponseDto>)로 변환
            - 여기서는 "최신 10개"만 변환
         */
        List<CommentResponseDto> commentDtoList = latestComments.stream()
                .map(CommentResponseDto::from)
                .toList();

        // 실제로 내려가는 최신 댓글 개수 (10개 또는 그 이하)
        int latestCommentsSize = commentDtoList.size();

        // 4) 해당 게시글의 Like 수 조회
        long likeCount = postLikeRepository.countByPostId(postId);

        /* 5) PostDetailResponseDto 로 통합 응답 생성
         - Post 엔티티 + 댓글 DTO 목록을 하나의 응답 객체로 묶어서 반환
         - 이번에는
            * 최신 댓글 10개 (latestComments)
            * 전체 댓글 개수 (totalCommentsCount)
            * 내려간 댓글 개수 (latestCommentsSize)
            * LIKE 개수 (likeCount)
           정보를 함께 내려줌
        */
        return PostDetailResponseDto.from(    // 파라미터 시그니처 변경
                post,                         // 게시글 정보
                commentDtoList,               // 최신 댓글 목록(최대 10개)
                totalCommentsCount,           // 전체 댓글 개수
                latestCommentsSize,            // 실제로 포함된 댓글 개수
                likeCount
        );
    }


    // 4. 최신 게시글 전체 조회 (페이징)
    public Page<PostResponseDto> getPosts(Pageable pageable){

        /* 4-1) 게시글 목록 조회
             - Post_id 기준 내림차순(최신글이 위로)
             - Pageable을 통해 page, size, sort 지정 가능
        */
        Page<Post> posts = postRepository.findByOrderByIdDesc(pageable);

        // 4-2) Page<Post> -> Page<PostResponseDto> 변환해서 반환
        // + 각 게시글 좋아요 수를 조회 하고 DTO에 함께 담아줌
        return posts.map(post -> {
            long likeCount = postLikeRepository.countByPostId(post.getId()); // 게시글별 LIKE
            return PostResponseDto.from(post, likeCount); // LIKE 수 포함 DTO 변환
        });
    }

    // 5. 작성자 기준 게시글 조회 (페이징)
    public Page<PostResponseDto> getPostsByAuthor(Long authorId, Pageable pageable){

        // 5-1) 작성자 ID(authorId) 기준 게시글 목록 페이징 조회
        Page<Post> posts = postRepository.findByAuthorId(authorId,pageable);

        // 5-2) DTO로 변환 후 반환 + 게시글 별 LIKE 수 포함
        return posts.map(post -> {
            long likeCount = postLikeRepository.countByPostId(post.getId()); // 게시글 LIKE 수 조회
            return PostResponseDto.from(post, likeCount);
        });
    }

    // 6. 제목 + 내용 키워드 검색 (IgnoreCase, 페이징)
    public Page<PostResponseDto> searchPosts(String keyword, Pageable pageable){

        // 6-1) keyword가 제목or내용에 포함된 글 검색
        Page<Post> posts = postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                        keyword, keyword, pageable
                );
        // 6-2) DTO로 변환 후 반환 + 게시글별 LIKE 수 포함하도록 람다식 변환
        return posts.map(post -> {
              long likeCount = postLikeRepository.countByPostId(post.getId());
              return PostResponseDto.from(post, likeCount);
                });
        /* 이전 코드
        return posts.map(PostResponseDto::from);
            PostResponseDto::from
                        ==
            “posts(Page<Post>) 안에 들어 있는 모든 Post를
            PostResponseDto.from(Post)로 변환해서
            Page<PostResponseDto> 로 만들어서 반환해라.”*/
    }

    // 7. 게시글 수정 -> 수정을 위해 별도 트랜잭션 필요
    @Transactional
    /* 7-1) 수정 게시글 조회
        - 게시글 없으면 예외
        - softDelete 적용으로 삭제된 글 조회 X
    */
    public PostResponseDto updatePost(Long postId,Long userId, String title, String content){

        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        // 작성자 검증
        if (!post.getAuthor().getId().equals(userId)){
            throw new IllegalStateException("작성자만 게시글을 수정할 수 있습니다.");
        }

        /* 7-2) 엔티티의 비지니스 메서드 사용하여 제목/내용 수정
            - Post.update(title,content) 내부에서 null,공백 체크 + trim 처리
            - JPA 변경 감지(Dirty Checking)에 의해 트랜잭션 종료시 자동으로 UPDATE 쿼리 실행
        */
        post.update(title,content);

        // 7-3) 수정된 엔티티를 DTO 변환 후 반환 + 수정 후 게시글 LIKE 수도 함께 전달
        long likeCount = postLikeRepository.countByPostId(postId);
        return PostResponseDto.from(post, likeCount);
    }

    // 추가 updatPost(이미지 수정 오버로드)
    @Transactional
    public PostResponseDto updatePost
    (Long postId, Long userId, String title, String content, MultipartFile image){
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        // 작성자 검증
        if (!post.getAuthor().getId().equals(userId)){
            throw new IllegalStateException("작성자만 게시글을 수정할 수 있습니다.");
        }

        // 1) 제목/내용 수정
        post.update(title, content);

        // 2) 이미지 처리: 파일이 없으면 기존 이미지 유지
        if (image != null && !image.isEmpty()) {
            String oldImagePath = post.getImagePath(); // 기존 이미지 백업
            String savedPath = fileStorageService.save(image); // 새 이미지 저장
            post.changeImage(savedPath); // DB에 새 경로 반영
            // 새 이미지 저장 성공 후 기존 파일 삭제
            if (oldImagePath != null
                    && !oldImagePath.isBlank()
                    && !oldImagePath.equals(savedPath)) {

                fileStorageService.delete(oldImagePath); // 실제 파일 삭제
            }
        }

        // 3) 반환 DTO
        long likeCount = postLikeRepository.countByPostId(postId);
        return PostResponseDto.from(post, likeCount);
    }

    // 8. 게시글 삭제 ( Soft Delete )
    @Transactional
    public void deletePost(Long postId, Long userId){
        /* 8-1) 삭제할 게시글 조회
            - softDelete 적용(@Where is_deleted=false)
            - 이미 삭제된 글(is_deleted=true)은 조회 X
        */
        Post post = postRepository.findById(postId)
                .orElseThrow(()-> new IllegalArgumentException("게시글을 찾을 수 없습니다. id="+postId));

        // 작성자 검증
        if (!post.getAuthor().getId().equals(userId)){
            throw new IllegalStateException("작성자만 게시글을 삭제할 수 있습니다.");
        }

        // 삭제 전 이미지 파일 같이 삭제
        String imagePath = post.getImagePath();
        if (imagePath != null && !imagePath.isBlank()) {
            fileStorageService.delete(imagePath);
        }

        post.delete();
        // JPA 변경 감지에 의해 트랜잭션 종료시 UPDATE쿼리 실행
        // ->> (is_deleted = true)로 변경
    }

}
