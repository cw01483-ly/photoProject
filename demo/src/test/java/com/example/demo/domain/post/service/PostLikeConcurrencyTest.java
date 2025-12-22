package com.example.demo.domain.post.service;

import com.example.demo.domain.post.repository.PostLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class PostLikeConcurrencyTest {

    @Autowired
    private PostLikeService postLikeService;
    // 실제 서비스 빈을 주입받아, 컨트롤러/보안 없이 비즈니스 로직만 직접 호출

    @Autowired
    private PostLikeRepository postLikeRepository;
    // 테스트 종료 후 DB 상태 확인용

    @Test
    @DisplayName("동시에 좋아요 토글 요청이 들어올 경우 동시성 문제 재현")
    void toggleLike_concurrency_test() throws InterruptedException {

        // 테스트에 사용할 고정 데이터
        Long postId = 331L;   // 타겟 게시글
        Long userId = 727L;   // 동일 사용자로 동시 요청

        int threadCount = 20;
        // 동시에 요청을 보낼 스레드 수

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        // 고정된 개수의 스레드를 동시에 실행하기 위한 스레드 풀

        CountDownLatch latch = new CountDownLatch(threadCount);
        // 모든 스레드 "동시 출발"을 위한 장치
        // 각 스레드는 작업이 끝나면 latch.countDown()을 호출

        // ===== 동시 요청 실행 =====
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postLikeService.toggleLike(postId, userId);
                    // 실제 운영에서 문제의 핵심이 되는 메서드를 직접 호출
                } catch (Exception e) {
                    // 동시성 충돌로 인한 예외 발생 시 콘솔에 출력
                    System.out.println("예외 발생: " + e.getClass().getSimpleName());
                    System.out.println(e.getMessage());
                } finally {
                    latch.countDown();
                    // 이 스레드의 작업이 끝났음을 알림
                }
            });
        }

        // 모든 스레드 끝날 때까지 대기
        latch.await();

        executorService.shutdown();
        // 스레드 풀 정리

        // 테스트 종료 후 DB 상태 출력
        long count = postLikeRepository.countByPostId(postId);
        System.out.println("최종 좋아요 개수 = " + count);
    }
}
