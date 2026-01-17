## Case4 / c4 – Result

---

## 1. 결과 요약

After 상태의 `comments` 테이블은 다음 조건을 충족한다.

| 항목 | Before | After |
|------|--------|-------|
| FK 컬럼 명 | author_id, user_id 혼재 | user_id 단일 |
| FK 참조 대상 | 의미 불명확 | users.id 명확 |
| FK constraint 이름 | 자동 생성 해시값 | fk_comments_user |
| post FK 이름 | 자동 생성 해시값 | fk_comments_post |
| JPA–DB 책임 | 혼재 | DB 메타데이터 기준 명확화 |

---

## 2. After 상태 검증

### 2-1. ERD 기준 구조 확인
- 중복되거나 의미가 혼동되는 FK 컬럼 없음

[ERD](erd/c4_01_erd_after.png)

---

### 2-2. FK constraint 이름 고정 여부 확인 

검증 쿼리:

```sql
SELECT
  CONSTRAINT_NAME,
  TABLE_NAME
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME = 'comments';
```

º [Before](resolution/c4_14_comments_fk_names_from_information_schema.png) <br>
º [After](db_after/c4_20_information_schema_comments_fk_after.png)

---

## 3. 결론

After 상태에서 comments 테이블은 다음과 같이 정리되었다.

- FK 컬럼 구조가 단일 의미로 정리되었고
- FK constraint 이름이 명시적 규칙으로 고정되었으며
- 운영, 분석, 문서화 관점에서 추적 가능한 상태가 되었다

