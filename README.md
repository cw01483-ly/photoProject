# 📷 PhotoProject


>PhotoProject는 성능 병목이 발생하기 쉬운 조회·페이징 구조를  
의도적으로 포함한 API를 기반으로,  
개선 전·후 성능 차이를 측정하고 검증하기 위해 설계된 백엔드 프로젝트입니다.

---

##  Performance Improvement Cases

아래는 실제 운영 환경을 기준으로  
조회 성능을 개선한 사례들을 Case 단위로 정리한 목록입니다.

---

### ● Case1. 조회 성능 개선 – N+1 Query 제거

- 게시글 목록 조회 성능 개선을 목표로 조회 구조 재설계
- 쿼리 수 고정 및 응답 시간(P95 / P99) 지표 개선으로 효과 검증

👉 [Case1 산출물 보기](./portfolio/case1)

---

### ● Case2. 페이징 조회 성능 개선 – Index & COUNT 최적화

- 페이징 조회 시 COUNT 쿼리 비용과 인덱스 설계의 영향 분석
- 인덱스 설계 변경을 통해 tail latency(P99) 개선 효과 검증

👉 [Case2 산출물 보기](./portfolio/case2)

---

### ● Case3. 설계 검증 – JPA 연관관계 주인 정합성

- 연관관계 주인 혼동으로 발생할 수 있는 정합성 리스크 점검
- ERD(FK 기준)와 테스트를 통해 현재 설계의 안정성 검증

👉 [Case3 산출물 보기](./portfolio/case3)
