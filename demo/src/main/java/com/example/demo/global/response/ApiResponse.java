package com.example.demo.global.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor

/* 공통 API 응답 형식 클래스
    - UserController,UserResponseDto 등 데이터를 감싸서 응답형태 통일
        -> 모든 컨트롤러의 응답을 동일한 구조로 통일
    - 성공,실패 여부(Success),메시지(Message),데이터(data)를 담음
*/
public class ApiResponse<T> {
    // 제네릭 사용 이유 : 다양한 타입에 대해 동일한 알고리즘을 적용, 코드를 재사용하기에 효과적

    //성공여부 (True/False)
    private final boolean success;

    //응답메시지 (로그인성공, 회원가입 실패)
    private final String message;

    //실제 응답 데이터 (Dto)
    private final T data;

    // 요청 성공시 사용하는 정적 메서드 ex) data : UserResponseDto
    public static <T> ApiResponse<T> success(T data, String message){
        //       선언부     리턴타입           매개변수타입
        return ApiResponse.<T>builder() // ApiResponse 객체를 빌더 패턴으로 생성
                .success(true)          // 성공여부 true
                .message(message)
                .data(data)
                .build();               // 최종 객체 생성 후 반환
    }

    // 요청이 성공했지만 데이터가 없는경우
    public static <T> ApiResponse<T> success(String message){
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)         // 성공은 했지만 데이터는 없는경우 이므로 null
                .build();
    }

    // 요청이 실패했을 때 사용하는 정적 메서드 ex) GlobalExceptionHandler 에서 사용
    // 실패 원인 메시지를 반환, data 자리에 ErrorResponse 객체가 들어감
    public static <T> ApiResponse<T> fail(T data,String message){
        return ApiResponse.<T>builder()
                .success(false)     // 요청 실패처리
                .message(message)   // 실패 메시지 설정
                .data(data)         // 에러 상세정보 그대로 담아줌
                .build();
    }

    // 메시지만 있고 data가 필요 없을 때 사용할 실패응답 (ex : ApiResponse.fail("권한이 없습니다")
    public static <T> ApiResponse<T> fail(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }

}
