# 4-1. 게시글 조회 성능 개선 (N+1) 검증 기록

## 0. 인증 전제 확인
- [캡처] Postman 로그인 성공 + Set-Cookie / Cookies HttpOnly 확인

## 1. Before 시나리오 A: 게시글 목록 조회
### 1-1) 요청 정보
- Endpoint: GET /api/posts?page=0&size