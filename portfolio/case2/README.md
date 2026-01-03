# Case2. 페이징 조회 성능 검증 (Index & COUNT)

## 1. 결과 요약

<!--
[작성 목적]
- 실험 종료 후, Case2의 결과를 사실 중심으로 요약
- 해석, 판단, 배경 설명은 작성하지 않음

[작성 내용]
- 인덱스 적용 전 / 후 실행계획 변화 요약
- p99 기준 지표 변화 요약 (수치 또는 방향성)

[주의]
- "왜"에 대한 설명 금지
- 결론적 서술 금지
- 사실 나열 중심
-->

---

## 2. 검증 대상과 범위
<!--
[검증 대상]
- GET /api/posts
- Spring Data JPA Page 기반 목록 조회
- Page 구조에서 자동 실행되는 COUNT 쿼리
- WHERE is_deleted = 0 조건

[검증 범위]
- COUNT 쿼리 실행계획(EXPLAIN)
- 인덱스 적용 전 / 후 비교
- 동일 조건 하 성능 지표 비교

[검증 제외]
- Page → Slice 전환
- COUNT 제거 또는 분리
- 사전 집계 구조
- API 응답 스펙 변경
-->

---

## 3. 사용한 핵심 지표
<!--
[작성 목적]
- 어떤 기준으로 "변화"를 판단할 것인지 명확히 정의
- Case1과 동일한 실험 결을 유지하되, Case2 특성 반영

[대표 지표]
- p99 latency

[보조 지표]
- p95 latency
- RPS

[현재 단계]
- 정량 지표 결론 없음
- 실행계획(EXPLAIN)을 1차 검증 기준으로 사용
-->

---

## 4. 대표 증거
<!--
[작성 목적]
- 이 Case에서 반드시 확보해야 할 증거를 목록으로 고정
- 실험 진행 중 누락 방지용 체크리스트 역할

[정적 증거]
- Page 조회 시 SQL 로그 (COUNT 포함)
- COUNT 쿼리 구조 확인
- EXPLAIN (Before)
- EXPLAIN (After)
- 인덱스 생성 전/후 SHOW INDEX

[동적 증거 - 추후]
- Grafana 지표 (Before / After)
- p99 기준 비교 스크린샷

실험 완료 후:
Case1처럼 대표 이미지 링크 섹션을 추가하는 것은 충분히 고려할 수 있음(변경가능)
-->

---

## 5. 산출물 위치 안내
<!--
[작성 목적]
- Case2 산출물의 물리적 위치를 고정
- README는 결과 설명서가 아니라 관문 역할

[경로]
-->
```text
portfolio/case2
├─ code
├─ explain
├─ grafana
├─ logs
├─ postman
└─ README.md
```
<!--
[폴더 역할]
- code     : 실험 대상 코드 스냅샷
- explain  : COUNT 쿼리 실행계획 전/후
- grafana : 성능 지표 스크린샷 (추후 추가)
- logs     : SQL 로그, 실행 기록
- postman : API 호출 재현용
-->

---

## 6. 안내 (Velog · Evidence)

본 Case의 **판단 과정과 선택 이유**는 기술 블로그(Velog)에 정리되어 있으며,  
**실험 결과에 대한 검증 증거(PDF)** 는 GitHub Case2 폴더에 별도로 제공한다.

- Velog
    - [Case2 Velog – ]()
- Evidence Pack (검증 증거 · 결과 원본)
    - [Case2_ _Query_ .pdf]( )