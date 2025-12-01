package com.example.demo.domain.user.dto;


/*
    - 회원가입 요청 시 클라이언트가 전송하는 데이터를 담는 DTO
    - Entity(User) 대신 사용하여 보안 향상(JSON 역직렬화)
    - 입력값 검증(@Valid)과 함께 사용
    */

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor //기본 생성자 자동 생성(JSON 역직렬화)
@AllArgsConstructor //모든 필드를 받는 생성자 자동 생성
@Builder
public class UserSignupRequestDto {

    //username
    @NotBlank(message = "ID는 필수 입력사항입니다.")
    @Pattern( //조건설정
            regexp="^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{4,20}$",
            message = "ID는 영문+숫자 조합 4~20자이며, 특수문자는 사용 불가합니다."
    )
    @Size(min = 4,max=20,message = "ID는 4 ~ 20자까지 가능합니다.")
    //regexp : 정규 표현식(Regular Expression) = 문자규칙
    // +: 수량자(quantifier)로, 앞의 패턴([A-Za-z0-9])이 한 번 이상 반복되어야 함을 의미
            /*
           ^                 : 문자열 시작
           (?= )             : 조건을 만족하는지 미리 검사하는 문법
           (?=.*[A-Za-z])    : 영문 최소 1개 포함
           (?=.*\d)          : 숫자 최소 1개 포함
           [A-Za-z\d]{4,20}  : 영문 또는 숫자로 구성되고 4~20자
           $                 : 문자열 끝
             */
    private String username; //로그인 ID username

    //password : DTO에서 정책 검증 수행(최소 8자, 영문/숫자 각1개 이상, 특수문자는 선택허용)
    @NotBlank(message = "비밀번호는 공백일 수 없습니다.")
    @Pattern( //영문&숫자 1개 이상씩, 특수문자는 선택적 허용, 공백사용 금지
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d!@#$%^&*()_\\-+=~]{8,64}$",
            message = "비밀번호는 영문과 숫자를 각각 1자 이상 포함해야 하며, 공백을 포함할 수 없습니다.."
     )
    @Size(min = 8, max =50 ,message = "비밀번호는 8~64자 까지 가능합니다.")
    private String password; //서비스계층에서 encode(암호화) 후 엔티티 설정

    //nickname : 한글,영문,숫자,언더바 허용, 최대 30자
    @NotBlank(message = "닉네임은 필수 입력사항입니다.")
    @Pattern(
            regexp = "^[A-Za-z0-9가-힣_]+$",
            message = "닉네임은 한글, 영문, 숫자, _(언더바)만 조합해 사용할 수 있습니다."
    )
    @Size (min = 4, max=30, message = "닉네임은 4 ~ 30자까지 가능합니다.")
    private String nickname;

    //email : 형식검증+공백금지, 중복여부는 서비스/DB에서 검사
    @NotBlank(message = "이메일은 필수 입력 사항입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max=100, message = "이메일은 최대 100자까지 가능합니다.")
    private String email;

}
