## Case4 / c2 — User SoftDelete + 연관 로딩 리스크 검증

---

## 검증 요약 

| 경로 | SoftDelete User 존재 시 결과                | 판정 |
|---|----------------------------------------|---|
| Post 단건 조회 | 404 반환                                 | 정책적 허용 |
| Comment 목록 조회 | USER: 200 + empty<br>ADMIN: 200 + list | 정책 분리 필요 |
| Comment 단건 조회 | Before: 500 발생<br>After: ADMIN 200     | 반드시 차단/보완 |

> **결론**  
> User SoftDelete는 연관 로딩 방식과 결합될 경우  
> API 경로별로 **404 / empty / 500**이라는 상이한 결과를 만들며,  
> 이는 설계 기준 부재로 인한 구조적 리스크로 판단한다.

---

## 관찰 조건

- User는 SoftDelete(is_deleted) 된다
- Comment.author(User)는 SoftDelete 될 수 있다
- 기본 연관 로딩은 LAZY
- 일반 사용자 API 경로에서 SoftDelete User는 JOIN 조건에 의해 연관 데이터 필터링 기준으로 작동한다

---

## DTO 사용 규칙

- 일반 사용자 : `CommentResponseDto`
- 관리자(ROLE_ADMIN): `CommentAdminResponseDto`
- 관리자 조회는 항상 Admin DTO 사용
- 관리자 응답에서 User JOIN / nickname 접근 금지
- 페이징 포함 예외 없음

---

## API별 응답 DTO

| API | USER | ADMIN |
|---|---|---|
| GET /api/posts/{postId}/comments | List\<CommentResponseDto> | List\<CommentAdminResponseDto> |
| GET /api/comments/{commentId} | CommentResponseDto | CommentAdminResponseDto |
| GET /api/posts/{postId}/comments/paging | Page\<CommentResponseDto> | Page\<CommentAdminResponseDto> |

---

## SoftDelete User 존재 시 응답 정책

### 1) GET /api/posts/{postId}

| 조건 | 응답 |
|---|---|
| 작성자 User SoftDelete | 404 |

---

### 2) GET /api/posts/{postId}/comments

#### USER

| 조건 | 응답 |
|---|---|
| SoftDelete User 댓글 존재 | 200 + empty list |

- SoftDelete User 댓글은 JOIN 조건에 의해 조회 결과에서 제외된다 (침묵형 제외)

#### ADMIN

| 조건 | 응답 |
|---|---|
| SoftDelete User 댓글 존재 | 200 + list |

- Admin DTO 사용
- User 엔티티 접근 금지

---

### 3) GET /api/comments/{commentId}

#### USER

| 조건 | 응답 |
|---|---|
| SoftDelete User 댓글 | 404 또는 접근 차단(정책 선택) |

- 500(서버오류) 금지

#### ADMIN

| 조건 | 응답 |
|---|---|
| SoftDelete User 댓글 | 200 |

- Admin DTO 사용
- User 엔티티 직접 접근 금지 (FK 기반)

---

## LazyInitializationException 방지 원칙

- 관리자 경로에서 User 엔티티 접근 금지
- JOIN FETCH로 해결하지 않음
- DTO 설계로 구조적으로 차단

---

## 결론

- SoftDelete User는 데이터는 유지, 조회는 정책으로 제어
- API별 응답 차이는 의도된 정책 결과
- 500은 정책 실패로 간주 (재발 금지)
