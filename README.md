### AI 활용

- 사용 AI 서비스: Claude (claude.ai 웹 채팅) / 구독 모델: Claude Sonnet 5.5 pro
- AI에게 받은 것: 서비스 주제·구조 설계 조언, 백엔드·프론트 코드 초안, 배포 중 오류 해결 안내
- 직접 한 것:
  - 코드를 읽고 수정, 로컬 Docker 실행 확인
  - EC2 생성, 보안 그룹 설정, 서버 배포
  - 배포 주소에서 기능 시험과 캡처
- 작업 중 만난 문제와 해결 (직접 확인한 것):
  - 로컬에서 6379 포트가 이미 사용 중이라 컨테이너가 안 켜짐 → compose에서 DB·Redis 포트 노출 제거
  - EC2 디스크 부족(no space left on device) → 정리 및 스왑 조정 후 재빌드
  - 학번 7자리 변경 후 "내 신청 내역"에서 검사 규칙이 안 맞던 문제 → RegistrationService.java에서 studentId.matches 범위 수정