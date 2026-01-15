# C-3 SoftDelete 경로 점검 (Before)

## 1) 목적
SoftDelete 실행 “트리거 경로”가 엔티티별로 혼재되어 있어,
프로젝트 전체에서 공식 삭제 경로를 1개로 고정한다.
(C-3에서는 entity.delete() 방식으로 단일화)

## 2) 현재 상태(혼재 증거)
- User: UserService.delete()에서 user.delete() 호출 (도메인 플래그 방식)
- Post: PostService.deletePost()에서 post.delete() 호출 (도메인 플래그 방식)
- Comment: CommentService.deleteComment()에서 commentRepository.delete(comment) 호출 (ORM delete 호출 방식)

## 3) 구조적 문제
- 엔티티(User/Post/Comment) 모두 @SQLDelete/@Where + delete()가 공존.
- 삭제 트리거가 엔티티마다 달라, 규칙이 사람 기억에 의존한다.

