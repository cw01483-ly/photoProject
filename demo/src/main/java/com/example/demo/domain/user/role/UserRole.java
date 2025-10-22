package com.example.demo.domain.user.role;

/*사용자 권한(Role)을 정의한 Enum
    -문자열 형태로 DB에 저장됨.
    -@Enumerated(EnumType.STRING) 덕분에 USER,ADMIN 같은 이름 그대로 저장 가능)
 */
public enum UserRole {

    USER,
    ADMIN
}
