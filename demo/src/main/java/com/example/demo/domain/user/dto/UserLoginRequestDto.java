package com.example.demo.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter // username,password에 대한 getter 자동생성
@NoArgsConstructor // JSON > 객체변환
@AllArgsConstructor // 모든 필드를 받는 자동 생성
@Builder //UserServiceTest에서 사용하기 위해 빌더 사용
//로그인 요청 데이터를 담는 Dto
public class UserLoginRequestDto {

    @NotBlank(message = "ID는 필수 입력 값입니다.")
    @Size(min = 4, max= 20,message = "ID는 3자 이상 20자 이하 입니다.")
    private String username;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하로 입력해주세요.")
    private String password;

}
