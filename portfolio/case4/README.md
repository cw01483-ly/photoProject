# Case4. 설계 검증 – JPA 연관관계 및 도메인 정합성 점검

본 문서는 Case4를 구성하는 각 하위 검증(c1~c4)의
검증 대상과 최종 판단을 요약한 허브 문서이다.

---

## c1. Comment–Post 연관관계 optional 정합성 검증

- 검증 대상
    - Comment → Post 연관관계 (`comments.post_id`)
- 핵심 점검 사항
    - DB NOT NULL 제약과 JPA `@ManyToOne(optional)` 표현의 일치 여부
- 검증 결과
    - DB는 필수 관계이나 JPA 표현이 선택 관계로 남아 있던 불일치 확인
    - `optional = false` 명시 및 테스트 고정으로 정책 동기화
- 최종 판정
    - **PASS** — 필수 관계 정책이 코드와 테스트로 보호됨

[c1 상세 검증 문서](c1/c1_audit.md)

---

## c2. User SoftDelete + 연관 로딩 리스크 검증

- 검증 대상
    - SoftDelete User와 연관된 Post / Comment 조회 경로
- 핵심 점검 사항
    - API 경로별 응답 불일치(404 / empty / 500) 발생 여부
    - LAZY 로딩 + JOIN 조건 결합 시 구조적 리스크
- 검증 결과
    - 일부 경로에서 침묵형 데이터 소실(empty) 및 500 가능성 확인
    - USER / ADMIN 응답 정책 분리 및 DTO 차단 규칙 확정
- 최종 판정
    - **RISK 관리 완료** — 정책 부재 상태에서 명시적 규칙 상태로 전환

[c2 상세 검증 문서](c2/c2_audit.md)

---

## c3. SoftDelete 실행 경로 단일화 점검

- 검증 대상
    - User / Post / Comment 삭제 트리거 경로
- 핵심 점검 사항
    - 삭제 실행 방식이 엔티티별로 혼재되어 있는지 여부
- Before 상태
    - 일부 엔티티에서 `repository.delete()`와 `entity.delete()` 혼재
- After 상태
    - 모든 엔티티에서 `entity.delete()` 단일 경로로 통일
    - `@SQLDelete` 제거, 도메인 플래그 방식 고정
- 최종 판정
    - **PASS** — 삭제 정책이 구조적으로 단일화됨

[c3 Before](c3/c3_00_softdelete_path_before.md)  
[c3 After](c3/c3_01_softdelete_path_after.md)

---

## c4. FK 네이밍 및 작성자 컬럼 정합성 점검

- 검증 대상
    - comments 테이블의 작성자 FK 구조 및 제약 조건
- 핵심 점검 사항
    - 작성자 의미 컬럼 중복 여부
    - FK 컬럼명 및 constraint 네이밍 규칙 적용 여부
- Before 상태
    - `author_id`, `user_id` 혼재
    - FK constraint 자동 생성 이름 사용
- After 상태
    - 작성자 FK 컬럼 `user_id` 단일화
    - FK constraint 명시적 규칙 적용
- 최종 판정
    - **PASS** — DB 메타데이터 기준으로 의미·규칙 명확화

[문제 정의](c4/c4_00_problem_definition.md)  
[해결 과정](c4/c4_01_resolution.md)  
[결과 정리](c4/c4_02_result.md)

