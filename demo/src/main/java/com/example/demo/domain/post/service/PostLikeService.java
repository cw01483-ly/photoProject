package com.example.demo.domain.post.service;

import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.entity.PostLike;
import com.example.demo.domain.post.repository.PostLikeRepository;
import com.example.demo.domain.post.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
    PostLikeService
    - 게시글 좋아요 관련 비즈니스 로직을 담당하는 서비스 클래스
    - 주요 기능
        1) 좋아요 토글 (누르면 추가, 다시 누르면 취소)
        2) 특정 게시글의 좋아요 개수 조회
        3) 특정 유저가 특정 게시글에 좋아요를 눌렀는지 여부 확인
 */
@Service
@RequiredArgsConstructor // final 필드를 매개변수로 받는 생성자를 롬복이 자동 생성
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostRepository postRepository; // 게시글 존재 여부, 엔티티 조회용
    private final UserRepository userRepository; // 사용자 존재 여부, 엔티티 조회용
    private final PostLikeRepository postLikeRepository; // 좋아요 엔티티 저장/조회/삭제용

    /*
        좋아요 토글 메서드
        - 이미 해당 user가 해당 post에 좋아요를 눌렀다면 -> 좋아요 취소(삭제)
        - 아직 누르지 않았다면 -> 좋아요 추가(생성)
        - 반환값(boolean) 의미:
            true  : 현재 호출 결과, "좋아요가 눌려진 상태"가 됨
            false : 현재 호출 결과, "좋아요가 취소된 상태"가 됨
     */
    @Transactional // 데이터 생성/삭제가 발생
    public boolean toggleLike(Long postId, Long userId) {
        // 1) 게시글 존재 여부 확인 (없으면 예외)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        // 2) 사용자 존재 여부 확인 (없으면 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));

        // 3) 기존에 좋아요가 있는지 조회
        return postLikeRepository.findByPostIdAndUserId(postId, userId)
                .map(existingLike -> {
                    // 3-1) 이미 좋아요가 존재하는 경우 -> 삭제(좋아요 취소)
                    postLikeRepository.delete(existingLike); // 엔티티 삭제
                    // 현재 상태는 "좋아요가 취소된 상태"이므로 false 반환
                    return false;
                })
                .orElseGet(() -> {
                    // 3-2) 좋아요가 아직 없는 경우 -> 새로 생성(좋아요 추가)
                    PostLike newLike = PostLike.builder()
                            .post(post) // 어떤 게시글에 대한 좋아요인지 설정
                            .user(user) // 어떤 사용자가 누른 좋아요인지 설정
                            .build();

                    postLikeRepository.save(newLike); // DB에 저장
                    // 현재 상태는 "좋아요가 눌려진 상태"이므로 true 반환
                    return true;
                });
    }

    /*
        getLikeCount
        - 특정 게시글에 달린 좋아요 개수 조회
        - 단순히 Repository의 countByPostId를 감싸는 래퍼 메서드
     */
    public long getLikeCount(Long postId) {
        // 게시글 존재 여부를 엄격히 체크하고 싶다면 아래 주석 해제 가능
        // postRepository.findById(postId)
        //         .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        return postLikeRepository.countByPostId(postId); // postId 기준으로 좋아요 개수 카운트
    }

    /*
        hasUserLiked
        - 특정 유저가 특정 게시글에 좋아요를 눌렀는지 여부 반환
        - 클라이언트에서 "좋아요 버튼 초기 상태"를 결정할 때 사용 가능
     */
    public boolean hasUserLiked(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId); // 존재 여부를 true/false로 반환
    }

}
