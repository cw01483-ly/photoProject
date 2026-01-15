package com.example.demo.domain.comment.entity;


import com.example.demo.domain.post.entity.Post;
import com.example.demo.domain.user.entity.User;
import com.example.demo.global.base.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) //파라미터 없는 기본 생성자 자동생성
@Where(clause = "is_deleted = false")
/*      @Where
         - Comment 엔티티 조회 시 항상 뒤에 붙을 조건절 지정
         - is_deleted = false 가 자동으로 붙기 때문에
            삭제된(is_deleted = true) 댓글은 조회 결과에서 빠짐
*/

public class Comment extends BaseTimeEntity {

    @Id //PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) //지연로딩
    // comment와Post의 관계 (N:1) = 여러개의 댓글 : 하나의 게시글
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comments_post"))
    private Post post; //게시글

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // comment와author의 관계 (N:1) = 여러댓글을 하나의 유저가 작성 가능
    // Comment 엔티티에서 User를 참조하는 연관관계
    @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comments_user"))
    private User author; //작성자

    /*댓글 내용 설정
        - 최소 한글자 이상(공백 제외)
        - 최대 길이 제한
    */
    @NotBlank(message = "댓글 내용은 공백일 수 없습니다.")
    @Size(max = 100, message = "댓글 내용은 최대 100자까지 입력 가능합니다.")
    @Column(name = "content", nullable = false, length = 100)
    private String content; //실 화면에서 보이는 댓글 내용 텍스트

    // 논리 삭제 플래그 ( true : 삭제된 상태( 사용자에게 보이지 않음)
    // 논리 삭제 플래그 ( false : 정상 상태)
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; //기본값은 false

    // 댓글 수정 메서드 > 도메인 로직을 엔티티 안에 둬서, Comment 객체가 스스로
    // 자신의 상태를 변경하도록 설계
    public void updateContent(String newContent){
        //파라미터 newContent가 null,공백 아니라면 content를 변경
        if (newContent != null && !newContent.isBlank()){
            this.content = newContent.trim();
        } //조건이 맞지 않으면 원래 내용 유지
    }

    /*
        논리 삭제 수행 메서드
        - 실제 DB 레코드를 물리적으로 삭제하지 않고
          isDeleted 값을 true 로 바꾸어 "삭제된 것처럼" 처리
          delete 호출 시에도 is_deleted 만 변경되도록 설계
    */
    public void delete() {
        this.isDeleted = true; // 삭제 상태로 플래그 변경
    }

    @Builder
    public Comment(Post post, User author, String content){
        this.post = post;
        this.author = author;
        this.content = content != null ? content.trim() : null;
        this.isDeleted = false; // 생성시점은 false 유지
    }
}
