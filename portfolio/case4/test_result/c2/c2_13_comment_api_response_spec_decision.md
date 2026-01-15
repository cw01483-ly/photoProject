# C-2 결정 산출물 – Comment API 응답 스펙

## DTO 사용 규칙
- 일반 사용자 : `CommentResponseDto`
- 관리자(ROLE_ADMIN): `CommentAdminResponseDto`

## API별 응답 DTO

| API | USER | ADMIN |
|---|---|---|
| GET /api/posts/{postId}/comments | List\<CommentResponseDto> | List\<CommentAdminResponseDto> |
| GET /api/comments/{commentId} | CommentResponseDto | CommentAdminResponseDto |
| GET /api/posts/{postId}/comments/paging | Page\<CommentResponseDto> | Page\<CommentAdminResponseDto> |

## 규칙 요약
- 관리자 조회는 항상 Admin DTO 사용
- 관리자 응답에서 User JOIN / nickname 접근 금지
- 페이징 포함 예외 없음
