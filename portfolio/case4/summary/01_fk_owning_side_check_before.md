# FK ↔ Owning Side 정합성 점검표 (Before)

기준:
- DB 기준선: ERD 상 FK 컬럼 위치
- JPA 기준선: owning side = @JoinColumn 보유 엔티티
- 저장 책임: Service 계층에서 FK를 세팅하고 save를 호출하는 주체

| 관계 | DB FK 위치(컬럼) | JPA Owning Side(=FK 보유) | Inverse Side | 저장 책임 | 판정 |
|---|---|---|---|---|---|
| Post → User(작성자) | posts.user_id | Post.author (@ManyToOne(optional=false) + @JoinColumn(name="user_id", nullable=false)) | 없음(단방향) | PostService.createPost: Post.builder().author(author) 후 postRepository.save(post) | OK |
| Comment → Post | comments.post_id | Comment.post (@ManyToOne(fetch=LAZY) + @JoinColumn(name="post_id", nullable=false)) | 없음(단방향) | CommentService.createComment: Comment.builder().post(post) 후 commentRepository.save(comment) | RISK |
| Comment → User(작성자) | comments.author_id | Comment.author (@ManyToOne(optional=false) + @JoinColumn(name="author_id", nullable=false)) | 없음(단방향) | CommentService.createComment: Comment.builder().author(author) 후 commentRepository.save(comment) | OK |
| PostLike → Post | post_likes.post_id | PostLike.post (@ManyToOne(optional=false) + @JoinColumn(name="post_id", nullable=false)) | 없음(단방향) | PostLikeService.toggleLike: PostLike.builder().post(post).user(user) 후 postLikeRepository.save(newLike) | OK |
| PostLike → User | post_likes.user_id | PostLike.user (@ManyToOne(optional=false) + @JoinColumn(name="user_id", nullable=false)) | 없음(단방향) | PostLikeService.toggleLike: PostLike.builder().post(post).user(user) 후 postLikeRepository.save(newLike) | OK |

## 판정 근거 메모

### RISK: Comment → Post
- DB 제약: comments.post_id 는 NOT NULL (nullable=false)
- JPA 매핑: @ManyToOne 에 optional=false 미명시 (기본값 optional=true)
- 결과: DB는 필수 관계인데, JPA 표현은 선택 가능처럼 오해될 여지가 있음
