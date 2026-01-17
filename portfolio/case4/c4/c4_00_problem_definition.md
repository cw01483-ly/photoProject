## Case4 / c4 — DB Problem Definition

---

### Observation Target (ERD)
- 테이블: comments
- 관련 컬럼:
  - author_id
  - user_id
- 관련 테이블: users, posts

[ERD](erd/c4_00_erd_before.png)

---

### Problem A — 작성자 컬럼 구조 중복

comments 테이블에는
작성자 의미로 해석될 수 있는 컬럼이 **2개** 존재하고,
이 중 Foreign Key 제약은 author_id 컬럼에만 적용되어 있다.

- author_id
- user_id

이는 작성자 식별 기준이
DB 구조 차원에서 **단일하지 않음을 의미한다.**

---

### Problem B — FK 네이밍 조건 미적용 (자동 생성 FK명 적용)

comments 테이블의 FK 제약은 DB에서 자동 생성된 이름으로 관측되며,
코드 의도(명시적 FK 제약명 규칙)가 DB에 반영되지 않은 상태.

즉, FK 제약명 네이밍이 “규칙 기반”이 아니라
“자동 생성(랜덤) 기반”으로 적용된 상태이다.

[comments_foreign_keys](db_before/c4_02_comments_foreign_keys.png) <br>
[nameSet](db_before/c4_02_comments_foreign_keys_nameSet.png)

---

### Problem C — 작성자 FK 컬럼명 불일치 (Post vs Comment)

- posts: user_id
- comments: author_id

동일 의미(작성자 FK)임에도 컬럼명이 다르게 존재하여,
DTO/쿼리/운영 관점에서 혼선을 유발할 수 있는 상태이다.

---

### Verdict

- 상태: **FAIL**
- comments 테이블의 작성자 FK 구조는
  단일 기준을 제공하지 못하는 상태이다
- 구조 정리가 필요하다
