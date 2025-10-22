package com.example.demo.domain.user.entity;

import com.example.demo.global.base.BaseTimeEntity;
import jakarta.persistence.*;// JPA 엔티티 관련 어노테이션(@Entity, @Id 등)
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import com.example.demo.domain.user.role.UserRole;
import java.time.LocalDateTime;


/*
    User엔티티 , 보안주체인 UserDetails는 따로 분리해서 구성할 예정
    DB에서 users 테이블로 매핑됨
 */
@Entity //엔티티클래스 선언
@Getter //모든 필드의 getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)// JPA가 프록시 생성을 위해 기본 생성자 필요 (protected 권장)
@AllArgsConstructor //모든 필드를 인자로 받는 생성자 자동 생성
@Builder //빌더 패턴 사용 가능(User.builder()...)
@Table(
        name = "users", //테이블 이름 지정 ,user는 SQL 예약어이기 때문에 직접 설정
        indexes = {
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_email", columnList = "email")
        },
        uniqueConstraints = { //중복 방지
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User extends BaseTimeEntity { //BaseTimeEntity 상속받아 시간설정 자동관리
    @Id //기본키(PK) 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY)//키값 자동 증가
    private Long id; // 고유 ID값

    //username
    @NotBlank(message="사용자명은 필수 입력사항입니다.")// 뷰 단계에서 공백 금지
    @Pattern(
            regexp = "^[A-Za-z0-9]+$", //regexp : 정규 표현식(Regular Expression) = 문자규칙
            // ^ : 문자열의 시작 $ : 문자열의 끝
            // +: 수량자(quantifier)로, 앞의 패턴([A-Za-z0-9])이 한 번 이상 반복되어야 함을 의미
            message = "사용자명은 영문+숫자 조합만 사용 가능합니다.(특수문자 불가)."
    )
    @Column(nullable = false, length=30) //DB단계 공백 금지, username수 제한 30
    private String username; // DB의 속성값

    //password
    @NotBlank(message = "비밀번호는 공백일 수 없습니다.") // 뷰 단계에서 공백 제한
    @Column(nullable = false, length=255)
    //비밀번호를 암호화할 경우를 대비에 길이를 여유있게 설정
    //비밀번호 조건검사는 Dto에서 작성할 예정,
    private String password;

    //e-mail
    @Setter
    @Email(message = "이메일 형식이 올바르지 않습니다") // 어노테이션을 활용하여 이메일 형식 검증
    @Pattern(
            regexp = "^\\S+$", //어떤 공백 문자도 허용하지 않음
            // \\S : 실제론 \S : 스페이스, 탭, 줄바꿈 등을 제외한 문자
            // + : 공백이 아닌 문자가 하나 이상 있어야함
            // 문자열 전체가 공백이 아닌 문자들로만 이루어져야 함을 명시.
            // @Email어노테이션을 사용하기에 굳이 한글제한까지 걸 필요는 없음
            message = "이메일에 공백을 포함 할 수 없습니다."
    )
    @NotBlank(message = "필수 입력 사항입니다.")
    @Column(nullable = false, length = 100)
    private String email;

    //jwc?
    //nickname
    @Setter
    @Pattern(
            regexp = "^[A-Za-z0-9가-힣_]+$",
            message = "닉네임은 한글,영문,숫자,_(언더바)만 사용하여 조합 할 수 있습니다."
    )
    @NotBlank(message = "닉네임은 공백일 수 없습니다.")
    @Column(nullable = false, length = 30)
    private String nickname; //닉네임

    // 프로필 이미지 관련 메서드(기본 이미지 경로 상수 설정)
    public static final String DEFAULT_PROFILE_IMAGE_URL = "/images/default_profile.png";

    //실제 이미지 경로 저장 필드(기본값 포함)
    @Column(name = "profile_image_url",length = 500,nullable = false)
    @Builder.Default
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
    private UserRole role; // 엔티티는 구조를 정의하는곳이기에 USER, ADMIN은 서비스 혹은 리퀘스트Dto에서 처리

    //계정활성화, Builder() 사용시에도 기본값 true 유지
    @Column(nullable = false)
    @Builder.Default// “DB에 보내기 전에 자바 객체가 null 상태로 만들어지지 않게 기본값을 넣어라”
    /*추후 UserPrincipal 을 통해 로그인 주체를 만들면
    Spring Security가 자동으로 해당 사용자의 enabled를 검사(true,false)
    false 라면 계정비활성화로 인식하여 자동으로 막음*/
    private boolean enabled = true; //계정 활성화 여부( true : 사용가능 )
    //즉 회원가입 당시에는 모든 회원이 계정활성화 상태임을 선언.
    //추후 관리자계정이 false로 바꾼다면 해당 계정은 비활성화됨


    //마지막 접속 시각
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /*추후 추가 예정
    * 1. @OneToMany(mappedBy = "author", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    * 2. private List<Post> posts = new ArrayList<>();*/

    //              도메인 비지니스 메서드

    //프로필 수정 (닉네임, 이메일)
    public void updateProfile(String newNickname, String newEmail){
        if(newNickname != null && !newNickname.isBlank()) this.nickname=newNickname.trim();
        if(newEmail != null && !newEmail.isBlank()) this.email=newEmail.trim();
    }
    /*if문이 독립적으로 동작하기 때문에 하나만 혹은 둘 다 변경해도 정상작동
    * .trim() : String(문자열)에서 불필요한 공백(스페이스,탭,줄바꿈 등) 을 잘라내는 메서드, 단 문자 사이의 공백은 제거안함
    * 하지만 메서드 실행시 nickname과 email메서드에서 걸러짐*/

    //비밀번호 변경하기 , 검증은 Service에서 수행
    public void changePasswordEncoded(String encodedPassword){
        if(encodedPassword == null || encodedPassword.isBlank()){
            throw new IllegalArgumentException("비밀번호가 유효하지 않습니다.");
        }
        this.password = encodedPassword;
    }
    /*public void changePassword(String currentRawPassword,
                               String newRawPassword,
                               PasswordEncoder passwordEncoder){
        *//*currentRawPassword : 사용자가 입력한 기존 평문 비밀번호
          newRawPassword     : 사용자가 입력한 새 평문 비밀번호
          passwordEncoder    : BCryptPasswordEncoder 등 스프링 시큐리티 *//*

        //1. 기존 비밀번호 일치하는지 검사
        if(!passwordEncoder.matches(currentRawPassword,this.password)){
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        //2. 새 비밀번호 형식 검증
        if(newRawPassword == null || newRawPassword.length()<8){
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }

        //3. 기존 비밀번호와 새 비밀번호 동일 여부 검사
        if(passwordEncoder.matches(newRawPassword,this.password)){
            throw new IllegalArgumentException("이전과 동일한 비밀번호는 사용할 수 없습니다.");
        }

        //4. 모든 조건문 통과시 새 비밀번호를 암호화 한 후 저장
        this.password = passwordEncoder.encode(newRawPassword);
    }
*/
    // 로그인 성공 시 마지막 로그인 시각 갱신
    public void markLoginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
