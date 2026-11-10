# 1단계: 데이터 설계와 API 명세

## 1. 테이블 설계 (ERD)

```
programs (프로그램)                         registrations (신청 기록)
┌──────────────────────────────┐          ┌──────────────────────────────┐
│ id              BIGINT  PK   │ 1      N │ id            BIGINT  PK     │
│ title           VARCHAR(100) │──────────│ program_id    BIGINT  FK     │
│ category        VARCHAR(20)  │          │ student_id    VARCHAR(20)    │
│ description     VARCHAR(500) │          │ student_name  VARCHAR(20)    │
│ location        VARCHAR(100) │          │ created_at    DATETIME       │
│ event_at        DATETIME     │          └──────────────────────────────┘
│ apply_start_at  DATETIME     │          UNIQUE (program_id, student_id)
│ apply_end_at    DATETIME     │          → 같은 학번이 같은 프로그램에 두 번 신청 불가
│ capacity        INT          │
│ created_at      DATETIME     │
└──────────────────────────────┘
```

- `category`: `LECTURE`(특강), `CAMP`(취업 캠프), `CONTEST`(공모전)
- 로그인 없이 **학번 + 이름**으로 신청자를 구분합니다.

## 2. Redis 키 설계

| 키 | 타입 | 의미 |
|---|---|---|
| `program:{id}:remaining` | String(숫자) | 남은 자리 수. `DECR`로 1씩 줄임 |
| `program:{id}:applicants` | Set | 신청한 학번 모음. `SADD` 결과로 중복 신청 판단 |

MySQL이 **원본 기록**, Redis는 **몰리는 순간의 빠른 판단용**입니다. Redis 키가 없으면 MySQL 기록으로 다시 채웁니다.

## 3. API 목록

| 기능 | 메서드 | 주소 | 성공 응답 |
|---|---|---|---|
| 서버 상태 확인 | GET | `/api/health` | 200 `{"status":"UP"}` |
| 프로그램 목록 조회 | GET | `/api/programs` | 200 프로그램 배열 |
| 프로그램 상세·현황 | GET | `/api/programs/{programId}` | 200 프로그램 1개 |
| 참가 신청 | POST | `/api/programs/{programId}/registrations` | 201 신청 결과 |
| 신청 취소 | DELETE | `/api/programs/{programId}/registrations` | 200 안내 메시지 |
| 내 신청 내역 | GET | `/api/registrations?studentId=20260001` | 200 신청 내역 배열 |

### 신청·취소 요청 본문

```json
{ "studentId": "20260001", "studentName": "김민규" }
```

### 프로그램 응답 예시

```json
{
  "id": 1,
  "title": "AI 시대의 개발자 커리어 특강",
  "category": "LECTURE",
  "categoryLabel": "특강",
  "location": "공학관 301호",
  "eventAt": "2026-11-20T14:00:00",
  "applyStartAt": "2026-11-10T09:00:00",
  "applyEndAt": "2026-11-17T23:59:00",
  "capacity": 30,
  "appliedCount": 28,
  "remainingSeats": 2,
  "status": "OPEN",
  "statusLabel": "신청 가능"
}
```

`status` 값: `UPCOMING`(신청 예정), `OPEN`(신청 가능), `FULL`(정원 마감), `CLOSED`(기간 종료)

### 오류 응답 형식

```json
{ "code": "PROGRAM_FULL", "message": "정원이 모두 찼습니다." }
```

| code | HTTP | 언제 |
|---|---|---|
| `INVALID_INPUT` | 400 | 학번·이름 형식 오류 |
| `INVALID_STUDENT_ID` | 400 | 내 신청 내역 조회 시 학번 형식 오류 |
| `PROGRAM_NOT_FOUND` | 404 | 없는 프로그램 번호 |
| `APPLY_NOT_STARTED` | 409 | 신청 시작 전 |
| `APPLY_CLOSED` | 409 | 신청 기간 종료 후 |
| `ALREADY_REGISTERED` | 409 | 같은 학번 중복 신청 |
| `PROGRAM_FULL` | 409 | 정원 초과 |
| `REGISTRATION_NOT_FOUND` | 404 | 신청하지 않은 학번으로 취소 |
| `NAME_MISMATCH` | 403 | 취소 시 이름 불일치 |
| `INTERNAL_ERROR` | 500 | 예상하지 못한 서버 오류 |
