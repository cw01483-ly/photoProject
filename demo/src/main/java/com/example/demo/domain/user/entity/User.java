package com.example.demo.domain.user.entity;

import com.example.demo.global.base.BaseTimeEntity;
import jakarta.persistence.*;// JPA 엔티티 관련 어노테이션(@Entity, @Id 등)
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import com.example.demo.domain.user.role.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore; //비밀번호 JSON직렬화 방지
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;


/*
    User엔티티 , 보안주체인 UserDetails는 따로 분리해서 구성할 예정
    DB에서 users 테이블로 매핑됨
    SoftDelete 적용 (삭제 시 is_deleted=true)
 */
@Entity //엔티티클래스 선언
@Getter //모든 필드의 getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)// JPA가 프록시 생성을 위해 기본 생성자 필요 (protected 권장)
/*@AllArgsConstructor
모든 필드를 인자로 받는 생성자 자동 생성 > 모든 필드를 외부에서 주입가능 > 무결성 저하 가능성이 있어서 제거함*/
@ToString(exclude = "password") // toString() 에서 password 제외
@Table(
        name = "users", //테이블 이름 지정 ,user는 SQL 예약어이기 때문에 직접 설정
        uniqueConstraints = { //DB에서 동일한 값 저장되는것을차단.
                //username,email 컬럼 각각에 대해 Unique Index를 생성 (무결성 보장)
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class User extends BaseTimeEntity { //BaseTimeEntity 상속받아 시간설정 자동관리
    @Id //기본키(PK) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)//키값 자동 증가
    private Long id; // 고유 ID값

    //username (영문+숫자조합, 길이 : 4~20, 숫자로만 작성 불가)
    @NotBlank(message="ID는 필수 입력사항입니다.")// 뷰 단계에서 공백 금지
    @Pattern(// @Pattern : 정규표현식(문자 조합 규칙)을 적용하여 검증
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{4,20}$",
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
            message = "ID는 영문+숫자 조합만 사용 가능합니다.(특수문자 불가)."
    )
    @Column(nullable = false, length=20) //DB단계 공백 금지, username수 제한
    private String username; // DB의 속성값

    //password
    @JsonIgnore //API 응답 등 직렬화 시 비밀번호 필드 제외
    @NotBlank(message = "비밀번호는 공백일 수 없습니다.") // 뷰 단계에서 공백 제한
    @Column(nullable = false, length=255)
    //비밀번호를 암호화할 경우를 대비에 길이를 여유있게 설정
    //비밀번호 조건검사는 Dto에서 작성할 예정,
    private String password;

    //e-mail
    @Email(message = "이메일 형식이 올바르지 않습니다") // 어노테이션을 활용하여 이메일 형식 검증
    @NotBlank(message = "필수 입력 사항입니다.")
    @Column(nullable = false, length = 100)
    private String email;


    //nickname
    @Pattern(
            regexp = "^[A-Za-z0-9가-힣_]+$",
            message = "닉네임은 한글,영문,숫자,_(언더바)만 사용하여 조합 할 수 있습니다."
    )
    @NotBlank(message = "닉네임은 공백일 수 없습니다.")
    @Column(nullable = false, length = 30, unique = true)
    private String nickname; //닉네임

    // 프로필 이미지 관련 메서드(기본 이미지 경로 상수 설정)
    public static final String DEFAULT_PROFILE_IMAGE_URL = "/images/default_profile.png";

    //실제 이미지 경로 저장 필드(기본값 포함)
    @Column(name = "profile_image_url",length = 500,nullable = false)
    private String profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;

    //프로필 이미지 교체 메서드
    public void changeProfileImage(String imageUrl){
        if(imageUrl==null || imageUrl.isBlank()){
            throw new IllegalArgumentException("공백일 수 없습니다.");
        }
        this.profileImageUrl=imageUrl.trim();
    }

    //프로필 이미지 제거 메서드, 삭제 시 기본 이미지 적용
    public void resetProfileImage(){
        this.profileImageUrl = DEFAULT_PROFILE_IMAGE_URL;
    }

    //현재 커스텀 이미지 사용 여부 확인
    public boolean hasCustomProfileImage(){
        return profileImageUrl != null && !DEFAULT_PROFILE_IMAGE_URL.equals(profileImageUrl);
    }


    //권한 설정
    @Enumerated(EnumType.STRING) //Enum 을 문자열로 DB에 저장할것을 JPA에게 알려줌
    //Enum은 한마디로 열거해놓은 선택지. 사용자를 일반사용자or관리자 어떤걸로 만들지
    /*role이라는 컬럼을 DB에 만들고 비워둘 수 없게 만든 후
    Role Enum에 정의된 값 중 하나를 문자열 형태(USER,ADMIN)로 저장하게 만드는것*/
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER; // 가입시 기본 권한을 일반 사용자로 지정.


    //계정활성화, Builder() 사용시에도 기본값 true 유지
    @Column(nullable = false)
    private boolean enabled = true; //계정 활성화 여부( true : 사용가능 )
    //즉 회원가입 당시에는 모든 회원이 계정활성화 상태임을 선언.
    //추후 관리자계정이 false로 바꾼다면 해당 계정은 비활성화됨


    /*
        SoftDelete 플래그
        - true  : 탈퇴(삭제)된 사용자
        - false : 정상 사용자
        - @Where 로 인해 기본 조회에서 is_deleted = true 유저 자동 제외
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;


    //마지막 접속 시각 필드
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 마지막 접속 시각 메서드.(성공시 UserService에서 호출)
    public void updateLastLoginAt(LocalDateTime lastLoginAt){
        this.lastLoginAt = lastLoginAt;
    }

    //              도메인 비지니스 메서드


    /*
    사용자 탈퇴 처리(SoftDelete)
        - 실제 DELETE 쿼리가 아닌 isDeleted 플래그 변경
        - UserService.delete()에서 호출
     */
    public void delete() {
        this.isDeleted = true;
    }

    //프로필 수정 (닉네임, 이메일)
    public void updateProfile(String newNickname, String newEmail){
        if(newNickname != null && !newNickname.isBlank()) this.nickname=newNickname.trim();
        if(newEmail != null && !newEmail.isBlank()) this.email=newEmail.trim();
    }
    /*if문이 독립적으로 동작하기 때문에 하나만 혹은 둘 다 변경해도 정상작동
    * .trim() : String(문자열)에서 불필요한 공백(스페이스,탭,줄바꿈 등) 을 잘라내는 메서드, 단 문자 사이의 공백은 제거안함
    * 하지만 메서드 실행시 nickname과 email메서드에서 걸러짐*/

    /*닉네임만 변경하고 싶을 때의 편의 메서드 (실제로는 updateProfile로 위임)*/
    public void changeNickname(String newNickname){
        updateProfile(newNickname,null);
    }

    /*이메일만 변경하고 싶을 때의 편의 메서드 (실제는 updateProfile로 위임)*/
    public void changeEmail(String newEmail){
        updateProfile(null,newEmail);
    }

    //비밀번호 변경하기 , 검증은 Service에서 수행
    public void changePasswordEncoded(String encodedPassword){
        if(encodedPassword == null || encodedPassword.isBlank()){
            throw new IllegalArgumentException("비밀번호가 유효하지 않습니다.");
        }
        this.password = encodedPassword;
    }


    // 로그인 성공 시 마지막 로그인 시각 갱신
    public void markLoginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
    }

    //정규화 훅(저장/수정 전 처리)
    /*username / email 정규화(트리밍 + 소문자화)
    * - 대소문자 혼용으로 인한 중복이나 로그인 혼선 방지
    * - 입력 경로가 달라도 일관된 보관 정책을 유지시키기 위함*/
    @PrePersist //엔티티가 DB에 "처음 저장되기 직전" 실행되는 이벤트(콜백)
    @PreUpdate //엔티티가 DB에 "수정되기 직전" 실행되는 이벤트(콜백)
    private void normalizeFields(){
        if (this.username !=null) this.username=this.username.trim().toLowerCase();
        //null이 아닐때만 실행, trim()으로 앞뒤 공백제거,소문자로 통일
        if (this.email !=null) this.email=this.email.trim().toLowerCase();
        if (this.nickname!=null) this.nickname=this.nickname.trim();
        // 닉네임은 화면에 보여지는 정보이므로 소문자 변환X , 앞뒤 공백만 제거
        if (this.profileImageUrl != null) this.profileImageUrl=this.profileImageUrl.trim();

    }

    @Builder
    /*"필드 선택 생성자"에 적용
     - 외부에서 id/role/enabled 등 민감 필드 임의 주입 방지
     - 가입에 필요한 필드만 받아 안전하게 생성
     - 기본 정책은 내부에서 강제 세팅(role = USER, enabled=true, 기본 프로필 이미지)
    */
    private User(String username, String password, String email, String nickname){
        this.username=username;
        this.password=password;
        this.email = email;
        this.nickname = nickname;

        this.profileImageUrl = DEFAULT_PROFILE_IMAGE_URL; //기본 프로필 이미지 적용
        this.role = UserRole.USER; //기본 권한 고정
        this.enabled=true; //기본 활성화 상태
    }


}
