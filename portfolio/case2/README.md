# Case2. 페이징 조회 성능 검증 – COUNT 쿼리 인덱스 실험

## 1. 결과 요약

- **실행계획 변화:** COUNT 쿼리의 `posts` 테이블 접근 방식이  
  `ALL`(Full Scan)에서 `ref`(Index Range Scan)로 전환되었으며,  
  검사 대상 행(rows) 수는 91,079건에서 45,539건으로 감소했다.

- **지표 변화:** Artillery 기준 평균 응답 시간은  
  1246.4ms에서 1179.8ms로 약 66.6ms 감소했고,  
  p95 응답 시간은 1436.8ms에서 1380.5ms로 약 56.3ms 감소했다.

- **관찰 사항:** 응답 시간의 하한선은 낮아졌으나,  
  p99 응답 시간은 1556.5ms에서 1525.7ms로 측정되어  
  상위 지연 구간(p99)에서는 수치 변화가 제한적으로 나타났다.


---

## 2. 검증 대상과 범위

### [검증 대상]
- **Endpoint:** `GET /api/posts`  
- **기술 스택:** Spring Data JPA `Page` 기반 목록 조회
- **핵심 요소:** `Page` 구조에서 자동으로 실행되는 COUNT 쿼리  
  (`is_deleted = 0` 조건이 고정된 상태)

### [검증 범위]
- **실행 계획:** `(is_deleted, id)` 복합 인덱스 추가 전·후 EXPLAIN 비교
- **성능 지표:** 동일 부하 조건 하에서의 응답 시간 측정
- **쿼리 구조:** `COUNT(DISTINCT p.id)`와 `LEFT JOIN post_likes`가 결합된 쿼리 형태

### [검증 제외]
- `Page` → `Slice` 전환
- COUNT 쿼리 제거 및 분리
- 사전 집계 구조 도입(미리 계산된 집계 값 사용)
- `PostLike` 연관관계 구조 및 집계 방식 변경
- API 응답 스펙 변경

---

## 3. 사용한 핵심 지표

### 핵심 판단 지표
- p99 latency

### 보조 지표
- p95 latency
- 평균 응답 시간
- RPS


---

## 4. 대표 증거

### 실행계획 증거
- [COUNT 쿼리 EXPLAIN (Before / After)](portfolio/case2/explain)
- [인덱스 생성 전·후 SHOW INDEX 결과](portfolio/case2/explain)

### 부하 테스트 증거
- [Artillery 부하 테스트 요약 결과](portfolio/case2/artillery)
- [Artillery 실행 로그 및 원본 결과](portfolio/case2/artillery)

### 모니터링 증거
- [/api/posts Grafana latency 스크린샷 (Before / After)](portfolio/case2/grafana)

### 쿼리 실행 증거
- [Page 조회 시 SQL 로그 (COUNT 쿼리 포함)](portfolio/case2/logs)



---

## 5. 산출물 위치 안내

본 Case에서 생성된 모든 산출물은 아래 경로에 정리되어 있다.

```text
portfolio/case2
├─ artillery  # 부하 테스트 시나리오 및 결과(요약/원본)
├─ code       # 실험 대상 코드 스냅샷
├─ explain    # COUNT 쿼리 실행계획(EXPLAIN) 전/후, 인덱스 정보
├─ grafana    # RPS·latency 모니터링 스크린샷
├─ logs       # /api/posts 요청 시 SQL 콘솔 로그(before/after)
├─ postman    # API 호출 재현 자료
└─ README.md
```

---

## 6. 안내 (Velog · Evidence)

본 Case의 **판단 과정과 선택 이유**는 기술 블로그(Velog)에 정리되어 있으며,  
**실험 결과에 대한 검증 증거(PDF)** 는 GitHub Case2 폴더에 별도로 제공한다.

- Velog
    - [Case2 Velog – COUNT 쿼리 병목: 페이징 조회 before/after 실험을 설계한 이유와 선택](https://velog.io/@cw01483/COUNT-%EC%BF%BC%EB%A6%AC-%EB%B3%91%EB%AA%A9-%ED%8E%98%EC%9D%B4%EC%A7%95-%EC%A1%B0%ED%9A%8C-beforeafter-%EC%8B%A4%ED%97%98%EC%9D%84-%EC%84%A4%EA%B3%84%ED%95%9C-%EC%9D%B4%EC%9C%A0%EC%99%80-%EC%84%A0%ED%83%9D)
- Evidence Pack (검증 증거 · 결과 원본)
    - [Case2_COUNT_Query_Index_Experiment_Evidence.pdf](./Case2_COUNT_Query_Index_Experiment_Evidence.pdf)