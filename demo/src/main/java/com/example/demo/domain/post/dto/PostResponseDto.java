package com.example.demo.domain.post.dto;


import com.example.demo.domain.post.entity.Post;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/*
    - 사용자가 화면에서 볼 수 있는 게시글 정보만 담는 응답용 DTO
    - Entity(Post)를 그대로 노출하지 않고 필요한 데이터만 선별해서 전달(보안)
*/
public class PostResponseDto {

    private Long id; // 게시글ID
    private Long displayNumber;
    private String title;
    private String content;
    private int views; //조회수
    private String authorName; //작성자(User엔티티 nickname)
    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime updatedAt; // 마지막 수정 시간
    private long likeCount; // 해당 게시글 Like 수

    @Builder
    private PostResponseDto(
            Long id,
            Long displayNumber,
            String title,
            String content,
            int views,
            String authorName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long likeCount
    ) {
        this.id = id;
        this.displayNumber = displayNumber;
        this.title = title;
        this.content = content;
        this.views = views;
        this.authorName = authorName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likeCount = likeCount;
    }

    //엔티티인 Post를 DTO로 변환하는 정적 메서드
    // -> 컨트롤러나 서비스에서 Post엔티티를 받아 해당 메서드로 DTO 변환
    // 기존 코드와의 호환성을 위해 likeCount를 0으로 두는 기본 버전 유지
    public static PostResponseDto from(Post post){
        return from(post, 0L); //Like 개수 알 수 없을 때, 기본값 0설정
    }

    public static PostResponseDto from(Post post, long likeCount){
        return PostResponseDto.builder()
                .id(post.getId())
                .displayNumber(post.getDisplayNumber())
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .authorName(post.getAuthor().getNickname()) //User엔티티에서 작성자명 추출(화면은 닉네임 노출)
                .createdAt(post.getCreatedAt())  // BaseTimeEntity에서 상속된 작성시각
                .updatedAt(post.getUpdatedAt())  // BaseTimeEntity에서 상속된 수정시각
                .likeCount(likeCount) //Like 개수 설정
                .build();
    }

}
