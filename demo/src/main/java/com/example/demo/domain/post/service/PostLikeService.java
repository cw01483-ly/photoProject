package com.example.demo.domain.post.service;

import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.post.entity.PostLike;
import com.example.demo.domain.post.repository.PostLikeRepository;
import com.example.demo.domain.post.repository.PostRepository;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException; // UNIQUE 충돌 예외 흡수
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException; // 동시 삭제/갱신 충돌 예외

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
        - 동시 요청(Concurrency) 상황에서,
            insert 중복(UNIQUE충돌) 또는 delete경합 같은 예외가 발생할 수 있으므로
            메서드는 예외를 "실패"로 두지 않고 흡수 후 DB 최종 상태(exists)를 재조회하여 liked값 결정

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

        // 존재 여부(exists)로 분기 후 "쿼리 삭제"를 수행
        boolean exists = postLikeRepository.existsByPostIdAndUserId(postId, userId); // 분기 기준

        if (exists) {
            // 좋아요가 이미 있으면 -> 쿼리 삭제로 취소
            try {
                postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            } catch (ObjectOptimisticLockingFailureException e) {
                // 동시성 상황: 다른 트랜잭션이 먼저 삭제했을 수 있음
                // 이미 취소된 것으로 간주하고 계속 진행
            } catch (Exception e) {
                // 예상 밖 예외도 동시성 케이스에서는 "취소 시도" 단계에서 흡수
            }
            // 최종 상태를 DB 기준으로 확정해서 반환
            return postLikeRepository.existsByPostIdAndUserId(postId, userId);
        }
        // exists == false 인 경우: 좋아요 추가 시도
        PostLike newLike = PostLike.builder()
                .post(post)
                .user(user)
                .build();

        try {
            postLikeRepository.save(newLike);
        } catch (DataIntegrityViolationException e) {
            // 동시성 상황: 다른 트랜잭션이 먼저 insert 했을 수 있음
            // 이미 좋아요 된 것으로 간주하고 계속 진행
        } catch (Exception e) {
            // 하이버네이트 세션/엔티티 상태 꼬임을 포함한 예상 밖 예외도 흡수
        }
        // 최종 상태를 DB 기준으로 확정해서 반환
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    /*
        getLikeCount
        - 특정 게시글에 달린 좋아요 개수 조회
        - 단순히 Repository의 countByPostId를 감싸는 래퍼 메서드
     */
    public long getLikeCount(Long postId) {
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
