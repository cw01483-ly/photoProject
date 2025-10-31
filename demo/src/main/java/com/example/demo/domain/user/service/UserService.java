package com.example.demo.domain.user.service;


import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List; //목록 반환용

@Service
@RequiredArgsConstructor // final필드(UserRepository)로 생성자를 자동으로 생성
@Transactional(readOnly = true) //기본적으로 읽기 전용 트랜잭션 ( 성능 최적화 )
public class UserService {
    private final UserRepository userRepository; // DB접근을 담당하는 Repository

    @Transactional //쓰기 작업이므로 readOnly=false 로 오버라이드
    public User register(User user){ // 회원 등록(Create)
        // 1) username 중복검사
        validateDuplicateUsername(user.getUsername()); //validateDuplicate : 중복검증

        // 2) eamail 중복 검사 (null 이 아닐때만 체크하기)
        if(user.getEmail() != null && !user.getEmail().isBlank()){
            validateDuplicateEmail(user.getEmail());
        }

        // 3) 저장, 검증 통과 시 Repository를 통해 저장하고, 영속화된 엔티티 반환
        return userRepository.save(user);
        /*추 후 확장 할 것들
        *  - PasswordEncoder 적용
        *  - 회원가입 이벤트 발행(알림/이메일 인증)*/
    }

    // username 중복검사 (DB확인)
    private void validateDuplicateUsername(String username){
        if (username == null || username.isBlank()){ // 기본 유효성 검사
            throw new IllegalArgumentException("ID는 필수 입력 사항입니다.");
        }
        boolean exists = userRepository.findByUsername(username).isPresent();// 중복 여부 확인
        if (exists){
            throw new IllegalArgumentException("이미 사용중인 ID 입니다 : " + username);
        }
    }

    // email 중복 검사 (DB확인)
    private void validateDuplicateEmail(String email){
        boolean exists = userRepository.findByEmail(email).isPresent(); // 중복 여부 확인
        if (exists){
            throw new IllegalArgumentException("이미 사용중인 E-mail 입니다 : "+ email);
        }
    }

    // 관리자,서버 내부 로직이 사용자를 식별할 경우, 권한은 추후 시큐리티에서
    public User getById(Long id){ //PK(id)로 단건조회(Read)
        // 미 존재시 EntityNotFoundException 던지기
        return userRepository.findById(id) // Optional<User> 반환
                .orElseThrow(()->new EntityNotFoundException("User not found. id = "+id)); // 값 없으면 예외
        /*
        * 컨트롤러/예외처리 레이어에서 한글화/표준 응답 변환 예정
        * (ex : 404 상태코드와 사용자 친화 메시지로 매핑할 것)*/
    }

    // 전체 조회(Read All), 관리자용
    public List<User> getAll(){ //List타입으로 빈 리스트반환[]이 가능하기에 elseTrow 불필요
        return userRepository.findAll(); //모든 User 목록 반환.
    }

    // username으로 조회(로그인 검증/관리기능 등 에서 사용 예정)
    public User getByUsername(String username){
        return userRepository.findByUsername(username) //Optional<User>
                .orElseThrow(()-> new EntityNotFoundException("User not found. username= "+username));
                //없으면 예외처리
    }

    //닉네임 변경
    @Transactional // 쓰기 트랜잭션(수정)
    public User updateNickname(Long id, String newNickname){ //닉네임 변경 : 유니크 중복검사 후 엔티티 위임
        User user = getById(id); // 존재 확인

        //  바꾸려는 닉네임이 기존과 다른때만 검사(트림결과 포함,불필요한 DB조회 방지)
        // 입력 정규화 : null허용X , 공백만있으면 무시, 값이 있으면 trim
        String trimmed = (newNickname == null) ? null : newNickname.trim(); //삼항연산자
        if(trimmed != null && !trimmed.isBlank() && !trimmed.equals(user.getNickname())){
            //닉네임 중복검사(자기 자신 제외)
            userRepository.findByNickname(trimmed).ifPresent(conflict -> {
                if (!conflict.getId().equals(id)) {
                    throw new IllegalStateException("이미 사용 중인 닉네임입니다: " + trimmed);
                }
            });
            user.changeNickname(trimmed); // 엔티티 상태변경
            /*내부적으로 updateProfile(trimmed, null) 위임하도록 구성*/
        }
        return user;
    }

    //이메일 변경, 유니크 중복 검사 후 엔티티 위임
    @Transactional
    public User updateEmail(Long id, String newEmail){
        User user = getById(id); // 대상 사용자 존재 확인

        String trimmed = (newEmail == null) ? null : newEmail.trim();
        if (trimmed != null && !trimmed.isBlank() && !trimmed.equals(user.getEmail())) {
            userRepository.findByEmail(trimmed).ifPresent(conflict -> {
                if (!conflict.getId().equals(id)) {
                    throw new IllegalStateException("이미 사용 중인 이메일입니다: " + trimmed);
                }
            });
            user.changeEmail(trimmed); // ← 엔티티 단일 진입점 재사용
        }
        return user;
        /* 확장 포인트
        *   - 이메일 변경 시 재인증 토큰 발송/검증 흐름연계*/
    }

    //삭제
    @Transactional
    public void delete(Long id){
        User user = getById(id);//존재 확인
        userRepository.delete(user); // 삭제 실행
        /*
            연관관계 정책 주의
                -Post 등 연관 엔티티가 생기면 orphanRemoval/CASCADE/FK 옵션 정책을 맞춰야함
                - 운영에서는 '탈퇴 처리(soft delete) 전략도 고려해볼 필요 있을듯.
         */
    }
}
