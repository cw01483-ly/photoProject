# Case2. 페이징 조회 성능 개선 (Index & COUNT)

## 1. Case 개요

본 Case는 게시글 페이징 조회 API에서  
**COUNT 쿼리 비용과 인덱스 설계 부재가 대량 데이터 환경에서 어떤 성능 리스크를 가지는지**를  
실행계획(EXPLAIN)과 성능 지표(Grafana)를 통해 검증한 사례이다.

Case1에서 다룬 N+1 쿼리 문제와 달리,  
본 Case는 **쿼리 개수가 아닌 단일 COUNT 쿼리의 실행 비용**에 초점을 둔다.

---

## 2. 결과 요약

게시글 페이징 조회 API는 기능적으로 정상 동작하고 있었으나,  
COUNT 쿼리가 인덱스를 활용하지 못하는 구조에서는  
데이터 증가 시 **tail latency(P99)가 급격히 악화될 수 있음을 확인하였다**.

인덱스 설계 변경 전·후를 동일한 조건에서 비교한 결과,  
실행계획과 P99 지표 모두에서 차이가 관측되었다.

---

## 3. 검증 대상과 범위

- 대상 API
    - `GET /api/posts` (게시글 페이징 목록 조회)

- 문제 유형
    - 페이징 조회 시 COUNT 쿼리 비용 과다
    - 인덱스 부재 또는 부적절한 인덱스 설계

- 본 Case에서 다루는 범위
    - COUNT 쿼리 실행계획(EXPLAIN)
    - 인덱스 설계 유무에 따른 실행계획 변화
    - Grafana 지표 변화 (특히 P99)

- 본 Case에서 다루지 않는 범위
    - N+1 쿼리 문제
    - 연관 엔티티 로딩 전략
    - DTO 변환 방식
    - JOIN 구조 자체의 변경

---

## 4. 대표 증거 (Before / After)

아래 모든 지표와 실행계획은  
**동일한 데이터 / 동일한 부하 조건 / 동일한 시간 구간**에서 수집되었다.

### 4-1. Grafana 지표 비교

#### RPS (Before / After)
- 처리량은 개선 전·후 동일 수준으로 유지됨

#### p95 Latency (Before / After)
- 일부 지연 구간에서 변화 관측

#### p99 Latency (Before / After)
- COUNT 쿼리 비용에 따른 tail latency 변화 확인

> Grafana 캡처는 동일 패널·동일 시간대 기준으로 비교한다.

---

### 4-2. COUNT 쿼리 EXPLAIN 비교

#### Before – COUNT 쿼리 실행계획

- posts 테이블
    - type: ALL
    - key: NULL
    - rows: 대량 스캔
- LEFT JOIN + `count(distinct post.id)` 구조
- 인덱스를 활용하지 못한 풀 스캔 발생

#### After – COUNT 쿼리 실행계획
- (인덱스 적용 후 실행계획 비교 결과 삽입)

---

## 5. 원인 정리 (실행계획 기준)

COUNT 쿼리는 페이징 조회 시 반드시 실행되며,  
인덱스를 활용하지 못할 경우 posts 테이블 전체를 스캔하게 된다.

특히,
- `type = ALL`
- `key = NULL`
- `count(distinct)` + `left join`

조합은 데이터 증가에 따라 COUNT 비용을 선형적으로 증가시키며,  
이는 평균 응답 시간보다 **tail latency(P99)에 더 큰 영향을 준다**.

---

## 6. 산출물 위치 안내

본 Case의 모든 산출물은 아래 경로에 정리되어 있다.

```text
portfolio/case2/
├─ Artillery/   # 동일 부하 조건 재현 결과
├─ Grafana/     # RPS / p95 / p99 지표 캡처
├─ explain/     # COUNT 쿼리 실행계획 전·후
├─ raw/         # SQL 로그 원본
├─ evidence/    # 비교용 정리 자료
└─ README.md    # 본 문서
