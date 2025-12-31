# Case1. 조회 성능 개선 (N+1)

## 실험 조건
- 부하 도구: Artillery
- 부하 방식: RPS 기반
- 테스트 시간: 60초
- 고정 RPS: 100

## Before 결과
- 상태: 완료
- 요약:
    - RPS ≈ 100
    - p95 ≈ 262ms
    - p99 ≈ 498ms
    - error = 0

## After 결과
- 상태: 미실행

## 산출물
- raw/: 실험 스크립트, 로그
- output/: 그래프 캡처, PDF
