## Case4 / c4 – FK 네이밍 및 컬럼 정합성 Resolution

---

## 요약

| Problem | 조치 내용 | 결과 |
|------|---------|------|
| A | comments.user_id 제거 | 작성자 컬럼 단일화 |
| B | 자동 생성 FK 제거 후 명시적 FK 재생성 | FK 네이밍 규칙 적용 |
| C | author_id → user_id 리네임 | 테이블 간 컬럼 의미 통일 |

---

## Problem A — 작성자 컬럼 정리

- 수행 작업
    - comments.user_id 컬럼 DROP
    - comments.author_id 를 작성자 기준 컬럼으로 유지
    - 이후 author_id → user_id 리네임

- 결과 상태
    - comments 작성자 FK 컬럼은 user_id 단일 존재

[drop_user_id](resolution/c4_10_comments_drop_user_id.png)

---

## Problem B — FK 네이밍 규칙 적용

- 수행 작업
    - information_schema 를 통해 기존 자동 생성 FK 확인
    - 기존 FK DROP
    - 명시적 이름의 FK 재생성
        - fk_comments_post
        - fk_comments_user

[fk_names](resolution/c4_14_comments_fk_names_from_information_schema.png)

- 결과 상태
    - comments 테이블 FK는 규칙 기반 이름 사용

[fk_named_change](resolution/c4_16_comments_fk_named_constraints_applied.png)

---

## Problem C — 작성자 FK 컬럼 명 통일

- 수행 작업
    - comments.author_id → user_id

- 결과 상태
    - posts / comments 모두 작성자 FK 컬럼명 user_id 사용

[after_rename.png](resolution/c4_13_show_columns_user_id_after_rename.png)

