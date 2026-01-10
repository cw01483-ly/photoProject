# Case3. COUNT 쿼리 구조 변경에 따른 페이징 조회 성능 비교

## 1. 결과 요약

- 대상 API: `GET /api/posts`
- 조회 방식: Spring Data JPA `Page` 기반 페이징
- 변경 사항: COUNT 쿼리 구조 변경
    - `post_likes` JOIN 제거
    - `DISTINCT` 제거


- 데이터 조건:
    - 게시글 수: 약 100,000건


- 부하 테스트 도구: Artillery  


- 테스트 조건:
    - RPS: 4
    - Duration: 120s
    - 동일 시나리오로 Before / After 측정

### 성능 측정 결과 (Artillery Summary)

| Metric | Before | After | Improvement |
|------|--------|-------|-------------|
| p95 | 2416.8 ms | 1022.7 ms | +57.7% |
| p99 | 3984.7 ms | 1176.4 ms | +70.5% |

---

## 2. 검증 대상과 범위

### [검증 대상]
- **Endpoint:** `GET /api/posts`
- **기술 스택:** Spring Data JPA `Page` 기반 목록 조회

### [변경 지점]
- Repository의 COUNT 쿼리
    - Before: `count(distinct post.id)` + `post_likes` LEFT JOIN
    - After: `count(post.id)` (posts 단독 조회)
- 목록 조회 쿼리
    - Before / After 동일 (JOIN + GROUP BY로 likeCount 집계)

### [검증 범위]

- 동일 API / 동일 페이징(Page) 유지
- 동일 데이터 상태 (posts 약 100,000건)
- 동일 부하 테스트 조건 (Artillery RPS 4, Duration 120s)
- 동일 모니터링 패널(Grafana p95/p99/RPS)
- 동일 검증 수단 사용
    - Artillery Summary
    - Grafana 캡처
    - MySQL EXPLAIN
    - Hibernate SQL 로그

- 검증 제외 범위
    - API 스펙 변경
    - 페이징 전략 변경 (Page → Slice 등)
    - 캐시/사전 집계/비동기 처리
    - 근사치 COUNT 사용
    - 인덱스 추가 실험(구조 변경 외 최적화)

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

### 부하 테스트 증거
- [Artillery Summary (Before / After)](artillery)

### 모니터링 증거
- [Grafana Dashboard (p95 / p99 / RPS)](grafana)

### GET /api/posts 단일 호출 로그
- [Hibernate SQL Log (COUNT 쿼리 구조 확인)](sql_log)

### EXPLAIN
- [MySQL EXPLAIN (COUNT 쿼리 구조 비교)](explain)


---

## 5. 산출물 위치 안내

본 Case에서 생성된 모든 산출물은 아래 경로에 정리되어 있다.
```text
portfolio/case3
├─ artillery  # 부하 테스트 시나리오 및 결과(요약/원본)
├─ code       # 실험 대상 코드 스냅샷(before/after)
├─ explain    # COUNT 쿼리 실행계획(EXPLAIN) 전/후
├─ grafana    # RPS·latency 모니터링 스크린샷
├─ sql_log    # /api/posts 요청 시 SQL 콘솔 로그(before/after)
└─ README.md
```

---


## 6. 안내 (Velog · Evidence)

본 Case의 **판단 과정과 선택 이유**는 기술 블로그(Velog)에 정리되어 있으며,  
**실험 결과에 대한 검증 증거(PDF)** 는 GitHub Case3 폴더에 별도로 제공한다.

- Velog
  - [Case3 Velog – 인덱스로 해결되지 않았던 COUNT 병목, 구조 변경이 해법이 될 수 있을까?](https://velog.io/@cw01483/%EC%9D%B8%EB%8D%B1%EC%8A%A4%EB%A1%9C-%ED%95%B4%EA%B2%B0%EB%90%98%EC%A7%80-%EC%95%8A%EC%95%98%EB%8D%98-COUNT-%EB%B3%91%EB%AA%A9-%EA%B5%AC%EC%A1%B0-%EB%B3%80%EA%B2%BD%EC%9D%B4-%ED%95%B4%EB%B2%95%EC%9D%B4-%EB%90%A0-%EC%88%98-%EC%9E%88%EC%9D%84%EA%B9%8C)
- Evidence Pack (검증 증거 · 결과 원본)
  - [Case3_COUNT_Query_Structure_Change_Evidence.pdf](./Case3_COUNT_Query_Structure_Change_Evidence.pdf)