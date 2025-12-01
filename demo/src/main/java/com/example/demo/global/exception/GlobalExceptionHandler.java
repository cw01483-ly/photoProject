package com.example.demo.global.exception;

import com.example.demo.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j // Lombok : log.warn(...), log.error(...) 를 바로 사용할 수 있게 Logger를 자동으로 추가
@RestControllerAdvice //모든 @RestController 에 적용되는 전역 예외처리기

/* 글로벌 예외 처리 핸들러
    - 컨트롤러에서 발생하는 예외를 한 곳에서 모아서 JSON 형태로 응답
 */
public class GlobalExceptionHandler {

    // 1) DTO 검증 실패 예외처리 (@Valid, @NotBlank, @Pattern, @Size 등)
    /**/
    @ExceptionHandler(MethodArgumentNotValidException.class)// 이 타입의 예외가 발생하면 이 메서드가 실행됨
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(
            MethodArgumentNotValidException ex,
            // 발생한 예외객체(어떤 필드에서 어떤 에러가 났는지 정보 포함)
            HttpServletRequest request
            // 어떤 요청 URL에서 에러가 났는지 알기위해 요청 정보도 함께 받음
    ){
        // 1-1) 기본 에러 메시지를 미리 하나 정해놓음 ( 아래에서 구분 못하면 해당 메시지 사용)
        String errorMessage = "요청 값이 올바르지 않습니다.";
        // 1-2) 예외 객체(ex) 안에는 "어떤 필드에서 어떤 메시지가 났는지" 정보가 들어있음
        //      getBindingResult().getFieldErrors() : 각 필드별 에러 목록
        if (!ex.getBindingResult().getFieldErrors().isEmpty()){ //필드 에러가 최소 1개 이상이라면
            // 첫 번째 에러를 가져와
            errorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
            /* -> DTO 필드에 설정한 message 값이 여기 들어옴
                예) @NotBlank(message = "비밀번호는 공백일 수 없습니다.")
                    -> "비밀번호는 공백일 수 없습니다." 가 defaultMessage가 됨
            */
        }
        /* 1-3) 서버 로그에 경고 레벨로 남기기 (문제 추척할 때 사용)
            - path : 어떤 URL에서 에러 났는지
            - message : 어떤 에러 메시지인지
        */
        log.warn("검증 실패 - path={}, message={}", request.getRequestURI(), errorMessage);

        // 1-4) 클라이언트에게 돌려줄 에러 응답 객체(ErrorResponse) 만들기
        //      -> 빌더 패턴 사용으로 가독성 ↑
        ErrorResponse body = ErrorResponse.builder()
                .success(false) // 성공여부 (false)
                .status(HttpStatus.BAD_REQUEST.value()) // HTTP상태 코드 400
                .message(errorMessage) //위에서 구한 에러메세지
                .path(request.getRequestURI()) //어떤 요청 URL에서 에러 발생했는지
                .timestamp(LocalDateTime.now()) //오류 발생 시간 기록
                .build();

        /* 1-5) ResponseEntity 사용하여
            - HTTP 상태코드 : 400 Bad Request
            - 응답 바디 : 위에서 만든 ErrorResponse JSON
            형태로 클라이언트에게 반환
            이제는 ErrorResponse를 그대로 반환하지 않고,
               ApiResponse.fail(...)로 한 번 감싸서 공통 응답 포맷 유지
        */
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) //응답 코드 400 설정
                .body(ApiResponse.fail(body, errorMessage));
        // body에 ErrorResponse 객체를 담아 ApiResponse 포맷으로 반환 -> JSON으로 변환되어 나감.
    }

    /* 2) IllegalArgumentException 처리 메서드
        - 서비스 레이어에서 throw new IllegalArgumentException() 한 경우 호출
        - 이 메서드가 그 예외를 잡아 400코드 + 예외메시지(JSON) 형식으로 응답
    */
    @ExceptionHandler(IllegalArgumentException.class) //IllegalArgumentException 발생 시 이 메서드 실행
    public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgumentException(
            IllegalArgumentException ex, //발생한 예외 객체( 예 : ex.getMessage() )
            HttpServletRequest request   // 요청 정보
    ){
        // 2-1) 서버 로그에 경고 레벨로 남기기
        log.warn("요청 처리 중 잘못된 인자 -path={}, message={}",
                request.getRequestURI(),
                ex.getMessage());
        // 2-2) 응답 바디로 사용할 ErrorResponse 객체 생성
        ErrorResponse body = ErrorResponse.builder()
                .success(false)                          // 실패이므로 false
                .status(HttpStatus.BAD_REQUEST.value())  // 400
                .message(ex.getMessage())                // 예외에 담긴 메시지를 그대로 사용
                .path(request.getRequestURI())           // 에러가 발생한 요청 경로
                .timestamp(LocalDateTime.now())          // 현재 시간
                .build();
        // 2-3) 400 상태코드와 함께 ErrorResponse를 응답으로 반환,
        // ApiResponse.fail(...)로 감싸 공통 포맷 유지
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(body, ex.getMessage()));
    }

    /* 3) 그 외 처리하지 않은 모든 예외 처리 메서드
        - 위에서 지정한 예외를 제외한 나머지 예외들인 경우 해당 메서드 사용
    */
    @ExceptionHandler(Exception.class) //최상위 예외타입.
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(
            Exception ex,
            HttpServletRequest request){
        // 3-1) 서버 내부 오류는 error 레벨로 로그 남기기, 추적가능하게
        log.error("서버 내부 오류 발생 -path={}, message={}",
                request.getRequestURI(),    //요청 경로
                ex.getMessage(),            //예외 메시지
                ex);                        // 예외객체 자체(스택트레이스 포함)

        // 3-2) 클라이언트가 보게 될 에러 응답 생성
        ErrorResponse body = ErrorResponse.builder()
                .success(false)
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
                .message("서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요.") // 사용자에겐 일반적인 안내만 보여주기
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        // 3-3) 500 상태코드로 응답 보내기,  ApiResponse.fail(...)로 감싸서 실패 응답도 공통 포맷 사용
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(body, "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }

    // 공통 에러 응답 포맷 클래스 -> 이 클래스를 JSON으로 변환 후 클라이언트에게 송출
    @Getter
    @Builder
    public static class ErrorResponse{
        private final boolean success; //API호출 성공,실패 확인
        private final int status;      // HTTP 상태코드 숫자(400,500 ...)
        private final String message;
        private final String path;     // 에러 발생한 요청 URL (ex : /api/users/login )
        private final LocalDateTime timestamp;

    }
}
