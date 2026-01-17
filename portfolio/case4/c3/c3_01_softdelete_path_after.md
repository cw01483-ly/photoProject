## Case4 / c3 — SoftDelete 경로 점검 (After)

## 1) 목적

SoftDelete 실행 “트리거 경로”를
프로젝트 전체에서 **entity.delete() 방식으로 단일화**한다.

---

## 2) 현재 상태 (정리 결과)

- User: UserService.delete() → user.delete() 호출 (도메인 플래그 방식)
- Post: PostService.deletePost() → post.delete() 호출 (도메인 플래그 방식)
- Comment: CommentService.deleteComment() → comment.delete() 호출 (도메인 플래그 방식)

→ 모든 엔티티에서 삭제 트리거 경로 동일

---

## 3) 구조적 정리 결과

- 엔티티(User/Post/Comment)에서 @SQLDelete 제거
- @Where(is_deleted = false) 유지
- 삭제 트리거 경로 1개로 고정 (entity.delete())

