# 📷 PhotoProject


>PhotoProject는 성능 병목이 발생하기 쉬운 조회·페이징 구조를  
의도적으로 포함한 API를 기반으로,  
개선 전·후 성능 차이를 측정하고 검증하기 위해 설계된 백엔드 프로젝트입니다.

---

## ⚫ Performance Improvement Cases

아래는 실제 운영 환경을 기준으로  
조회·페이징·설계 리스크를 검증한 성능 개선 사례들입니다.
---

### ⬤ Case1. 조회 성능 개선 – N+1 Query 제거

- 게시글 목록 조회 시 발생한 N+1 쿼리 문제를 해결
- 쿼리 수 고정 및 tail latency(p95 / p99) 개선 여부를 지표로 검증

👉 [Case1 README 보기](portfolio/case1/README.md)

---

### ⬤ Case2. 페이징 조회 성능 검증 – COUNT 쿼리 인덱스 실험

- 페이징 조회 시 COUNT 쿼리 비용과 인덱스 설계의 영향 분석
- 인덱스 설계 변경으로 응답 시간 분포의 소폭 개선 확인
- (한계) COUNT 구조의 근본 병목은 Case3에서 “구조 변경”으로 분리하여 해결

👉 [Case2 README 보기](portfolio/case2/README.md)

---

### ⬤ Case3. COUNT 쿼리 구조 변경에 따른 페이징 조회 성능 비교

- Case2에서 확인된 COUNT(JOIN/ DISTINCT 등) 병목을 근본적으로 제거하는 구조 변경
- “인덱스 개선”이 아닌 “쿼리/조회 구조 재설계”로 tail latency 개선

👉 [Case3 README 보기](portfolio/case3/README.md)


---

### ⬤ Case4. 설계 검증 – JPA 연관관계 및 도메인 정합성 점검

- 성능 개선 이후 단계에서 남을 수 있는 설계·정합성 리스크를 단계별로 검증
- 연관관계 주인, Soft Delete, FK 네이밍 등 주요 설계 축을 c1~c4 하위 Case로 분리하여 점검
- ERD, 테스트, 코드 흐름을 기준으로 현재 도메인 설계의 안정성을 종합 확인

👉 [Case4 README 보기](portfolio/case4/README.md)
