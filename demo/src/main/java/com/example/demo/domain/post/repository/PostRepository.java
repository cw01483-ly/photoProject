package com.example.demo.domain.post.repository;

import com.example.demo.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/*
    - Post엔티티에 대한 DB접근(CRUD)을 담당하는 Jpa Repository
    - JpaRepository<Post, Long> 상속으로 기본 CRUD + 페이징/정렬 기능 자동 제공

    ️ Soft Delete(@Where(is_deleted=false))와의 상호작용
    - 엔티티에 @Where(clause="is_deleted=false")가 걸려 있으면
     모든 JPQL/메서드 쿼리에서 삭제된 레코드가 자동 제외
    - 단, 네이티브 쿼리(nativeQuery=true)에는 @Where가 적용 X

    interface 는 객체의 메서드에 대한 규칙을 정의,선언
*/
public interface PostRepository extends JpaRepository<Post, Long> {

    // 1) displayNumber 채번용 (SoftDelete 사용으로 삭제글 생길 시 자동 제외)
    @Query("select coalesce(max(p.displayNumber),0) from Post p")
    /* coalesce(...,0) : 여러 인수를 순서대로 검사, null이 아닌 첫 번째 인수를 반환
          (1)max(p.displayNumber)의 결과를 확인
          (2)만약 max()의 결과가 숫자로 존재하면 그 숫자를 반환
          (3)만약 max()의 결과가 NULL이면 두번째 인수인 0 반환
      >> (2)인 경우 가장 큰 displayNumber반환. 다음값은 displayNumber+1
      >> (3)인 경우 coalesce 덕분에 0을 반환. 다음값은 1이 되어 안전하게 시작
    */
    Long findMaxDisplayNumber();

    // 2) 작성자 기준 조회(페이징)
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);
    //                         FK:user_id     페이지 정렬정보



    // 3) 제목+내용 키워드 검색 (대소문자 무시, 페이징)
    Page<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String titlekeyword, String contentkeyword, Pageable pageable
    );

    // 4) 최신순 전체 목록 (페이징)
    Page<Post> findByOrderByIdDesc(Pageable pageable);

    // 5) 단건 조회 + 조회수 증가 (벌크 업데이트)
    @Modifying(clearAutomatically = true,flushAutomatically = true)
    @Query("update Post p set p.views = p.views + 1 where p.id = :id")
    int increaseViews(@Param("id") Long id);

    // 6) 삭제여부 무시하고 단건조회(관리자용, @Where 우회)
    @Query(value = "select * from posts where id = :id", nativeQuery = true)
    Optional<Post> findRawById(@Param("id") Long id);

}
