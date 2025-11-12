package com.example.demo.domain.post.dto;


import com.example.demo.domain.post.entity.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
/*
    - 사용자가 화면에서 볼 수 있는 게시글 정보만 담는 응답용 DTO
    - Entity(Post)를 그대로 노출하지 않고 필요한 데이터만 선별해서 전달(보안)
*/
public class PostResponseDto {

    private Long id; // 게시글ID
    private String title;
    private String content;
    private int views; //조회수
    private String authorName; //작성자(User엔티티 username)
    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime updatedAt; // 마지막 수정 시간

    //엔티티인 Post를 DTO로 변환하는 정적 메서드
    // -> 컨트롤러나 서비스에서 Post엔티티를 받아 해당 메서드로 DTO 변환

    public static PostResponseDto from(Post post){
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .authorName(post.getAuthor().getUsername()) //User엔티티에서 작성자명 추출
                .createdAt(post.getCreatedAt())  // BaseTimeEntity에서 상속된 작성시각
                .updatedAt(post.getUpdatedAt())  // BaseTimeEntity에서 상속된 수정시각
                .build();
    }

}
