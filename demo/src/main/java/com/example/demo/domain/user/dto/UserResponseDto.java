package com.example.demo.domain.user.dto;


import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.role.UserRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //외부 임의 생성제한을 위해 프로텍트
@AllArgsConstructor
@Builder
/*클라이언트 응답전용 DTO
*   - 필요한 정보만 골라 보냄(캡슐화)
*   - password, enabled(내부정책) 등 민감필드 제외
*   - Controller/Service에서 User -> UserResponseDto 변환하여 반환
*/
public class UserResponseDto { //응답에 포함될 필드만 담는 상자

    //외부에 노출되어도 안전한 기본 프로필
    private Long id; //사용자 고유 식별자 PK

    private String username;

    private String nickname;

    private String email;

    private String profileImageUrl;

    private UserRole role;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    // 정적 팩토리 메서드 : User엔티티 -> 응답DTO 변환
    public static UserResponseDto from(User user){ //엔티티 받아서 Dto로 바꾸는 표준 진입점

        return UserResponseDto.builder() //빌더 시작
                .id(user.getId())
                .username(user.getUsername()) //정규화(소문자,trim)는 Entity저장 시점에 이미 적용
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build(); //빌더 완료

    }
}
