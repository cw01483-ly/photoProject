## Case4 / c1 — Comment–Post optional 정합성 검증

### 대상
- 엔티티 관계: Comment → Post
- 컬럼: comments.post_id
- JPA 매핑: Comment.post (@ManyToOne)

---

### 기준
- DB 기준: `comments.post_id`는 NOT NULL
- JPA 기준: 필수 관계는 `@ManyToOne(optional = false)`로 표현되어야 한다
- 정책 검증 방식: 테스트로 optional 값 고정

---

### Before (FAIL)

- JPA 매핑:
    - `@ManyToOne(fetch = LAZY)`
    - `optional` 미지정 (기본값 true)
- 문제:
    - DB는 필수 관계이나 JPA 표현은 선택 관계처럼 보임
- 검증 결과:
    - `comment_post_manyToOne_optional_shouldBeFalse` 테스트 실패
    - Assertion: expected false but was true

---

### Fix

- JPA 매핑 수정:
    - `@ManyToOne(fetch = LAZY, optional = false)`
    - DB NOT NULL 제약과 JPA 의미 동기화

---

### After (PASS)

- 동일 테스트 재실행
- `comment_post_manyToOne_optional_shouldBeFalse` 테스트 통과
- 필수 관계 정책이 테스트로 고정됨

---

### Verdict

- 상태: **PASS**
- 결과:
    - Comment → Post 관계의 필수성(DB/JPA 표현 불일치) 해소
    - 필수 관계 정책이 테스트로 보호됨
