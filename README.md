# 교내 비교과 프로그램 신청 서비스

> ✏️ 표시가 있는 곳은 **직접 실행해서 확인한 값**으로 바꿔 주세요.

## 1. 서비스 소개

**대상 사용자**: 학교에서 열리는 특강, 취업 캠프, 공모전에 참여하려는 재학생

**핵심 기능**

| 기능 | 설명 |
|---|---|
| 프로그램 목록 조회 | 종류별(특강·취업 캠프·공모전)로 프로그램과 일시·장소·마감 시각 확인 |
| 참가 신청 | 학번·이름으로 선착순 신청, 정원·신청 기간·중복 여부 검사 |
| 신청 취소 | 학번·이름이 일치할 때 신청 취소, 자리 즉시 반환 |
| 신청 현황 확인 | 칸 하나가 자리 하나인 좌석 표시, 잔여석·상태 10초마다 자동 갱신 |

**트래픽 상황**: 인기 특강 신청이 시작되는 순간 접속과 신청 요청이 짧은 시간에 몰립니다. 이때 정원을 넘겨 신청되지 않도록 정원 판단을 Redis에서 처리합니다.

## 2. AWS 구성도

```
                        ┌──────────────── EC2 (Ubuntu, Docker Compose) ────────────────┐
                        │                                                              │
 브라우저 ── HTTP :80 ──▶ │  Nginx :80 ──(/api/*)──▶ Spring Boot :8080 ──┬──▶ MySQL :3306  │
                        │     │                                       └──▶ Redis :6379  │
                        │     └── 화면 파일(HTML/CSS/JS) 제공                              │
                        └──────────────────────────────────────────────────────────────┘
   보안 그룹 인바운드: 80(전체), 22(내 IP만) / MySQL·Redis 포트는 외부에 열지 않음
```

| 구성 요소 | 역할 | 선택 이유 |
|---|---|---|
| EC2 | 컨테이너 4개를 실행하는 가상 서버 | 수업에서 만든 Docker Compose 구성을 그대로 올릴 수 있음 |
| Nginx | 화면 제공, `/api/` 요청을 Spring Boot로 전달 | 화면과 API를 같은 주소로 제공 (CORS·HTTPS 혼합 문제 없음) |
| Spring Boot | 신청·취소·조회 API | 팀에 익숙한 백엔드 |
| MySQL | 프로그램·신청 기록의 원본 저장 | 신청 기록을 영구 보관, 학번 중복을 DB 제약으로 한 번 더 차단 |
| Redis | 잔여석 카운터(`DECR`), 신청자 집합(`SADD`) | 원자적 명령이라 동시 요청에도 정원 초과가 생기지 않음 |

### 요청 흐름 (참가 신청)

```
브라우저 POST /api/programs/1/registrations
 → Nginx → RegistrationController → RegistrationService
   ① 프로그램 존재·신청 기간 확인 (MySQL)
   ② SADD program:1:applicants 학번   → 0이면 "이미 신청"
   ③ DECR program:1:remaining         → 0 미만이면 되돌리고 "정원 마감"
   ④ registrations 테이블에 저장 (MySQL)
 ← 201 Created
```

### ELB·Auto Scaling 검토

이번 배포에서는 **적용하지 않았습니다.**

신청이 몰리는 시간은 신청 시작 직후 몇 분으로 짧고, 평소 이용량은 적습니다. 로드 밸런서는 켜 둔 시간만큼 비용이 계속 나가므로 과제 규모에서는 비용 대비 효과가 낮다고 판단했습니다. 대신 정원 판단을 서버 메모리가 아닌 **공유 Redis**에 두었기 때문에, 나중에 ALB + Auto Scaling으로 Spring Boot 서버를 여러 대로 늘려도 모든 서버가 같은 잔여석을 보고 판단하여 정원 초과 없이 그대로 동작합니다. 확장 시에는 MySQL을 RDS로, Redis를 ElastiCache로 분리하고 앱 서버만 Auto Scaling 그룹에 넣는 구조를 생각하고 있습니다.

## 3. 실행·배포

### 프로젝트 구조

```
campus-program/
├── docker-compose.yml      # nginx, app, mysql, redis 4개 컨테이너
├── .env.example            # 환경변수 예시 (복사해서 .env로 사용)
├── nginx/default.conf      # 화면 제공 + /api/ 전달 설정
├── frontend/               # index.html, style.css, app.js
├── backend/                # Spring Boot (Dockerfile 포함)
├── scripts/concurrency-test.sh   # 동시 신청 시험
└── docs/API.md             # 테이블 설계·API 명세
```

### 로컬 실행

준비: Docker Desktop

```bash
cp .env.example .env
docker compose config          # 설정 파일 문법 검사
docker compose up -d --build   # 첫 빌드는 몇 분 걸림
curl http://localhost/api/health
# {"status":"UP"}
```

브라우저에서 `http://localhost` 접속

### EC2 배포

1. EC2 인스턴스 생성: Ubuntu 24.04, ✏️ 인스턴스 유형(예: t3.small)
2. 보안 그룹 인바운드: HTTP 80 (0.0.0.0/0), SSH 22 (내 IP)
3. 접속 후 설치

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu
exit   # 다시 접속해야 docker 권한 적용
```

4. (메모리 1GB 인스턴스라면) 빌드 중 메모리 부족을 막기 위해 스왑 추가

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

5. 실행

```bash
git clone ✏️<저장소 주소>
cd campus-program
cp .env.example .env
nano .env                      # 비밀번호 변경
docker compose up -d --build
docker compose ps              # 4개 모두 running 확인
curl http://localhost/api/health
```

6. 접속 주소: ✏️ `http://<EC2 퍼블릭 IP>`

| 포트 | 용도 | 외부 공개 |
|---|---|---|
| 80 | Nginx (화면 + API) | O |
| 8080 | Spring Boot | X (컨테이너 내부) |
| 3306 | MySQL | X (서버 내부 127.0.0.1만) |
| 6379 | Redis | X (서버 내부 127.0.0.1만) |

초기 데이터: 서버 첫 실행 시 시연용 프로그램 5개가 자동으로 추가됩니다(신청 가능, 정원 3명, 신청 예정, 기간 종료, 동시성 시험용).

## 4. 검증

시험 환경: ✏️ 배포 주소 `http://...`, 브라우저 ✏️(예: Chrome), 시험 날짜 ✏️

| 기능 | 구분 | 입력 | 기대 결과 | 실제 결과 |
|---|---|---|---|---|
| 목록 조회 | 정상 | 첫 화면 접속 | 프로그램 5개, 상태별 버튼 표시 | ✏️ |
| 목록 조회 | 경계 | "공모전" 선택 | 공모전 1개만 표시 | ✏️ |
| 참가 신청 | 정상 | 1번에 학번 2026001, 이름 입력 후 신청 | "신청했습니다", 잔여석 1 감소 | ✏️ |
| 참가 신청 | 실패 | 학번 `123` 입력 후 신청 | "학번은 숫자 7자리로 입력해 주세요." | ✏️ |
| 참가 신청 | 실패 | 정원 3명인 2번에 4번째로 신청 | "정원이 모두 찼습니다." | ✏️ |
| 신청 취소 | 정상 | 신청한 프로그램에서 신청 취소 | "신청을 취소했습니다", 잔여석 1 증가 | ✏️ |
| 신청 취소 | 실패 | 이름을 다르게 입력하고 취소 | "신청할 때 입력한 이름과 일치하지 않습니다." | ✏️ |
| 신청 현황 | 정상 | 브라우저 B에서 신청 | 브라우저 A 화면이 10초 안에 칸·잔여석 갱신 | ✏️ |
| 신청 현황 | 경계 | 3번(신청 예정) 확인 | "신청 예정"과 시작 시각, 신청 버튼 없음 | ✏️ |

### 동시 신청 시험

```bash
bash scripts/concurrency-test.sh http://<EC2 퍼블릭 IP> 5 100
```

| 항목 | 기대 | 실제 |
|---|---|---|
| 성공(201) | 30건 | ✏️ |
| 정원 마감(409) | 70건 | ✏️ |
| 서버 잔여석 | 0 | ✏️ |

시험을 다시 하려면 `docker compose down -v && docker compose up -d --build` 로 데이터를 초기화합니다.

## 5. 기록

**팀 역할**: ✏️

**AI 활용**: ✏️ 일반 AI 채팅 도구(에이전틱 코딩 도구 미사용)를 사용한 부분과 직접 수정·검증한 부분을 구체적으로 기록

**검사 후 자원 정리**: ✏️ 검사 완료 후 EC2 인스턴스 종료, 연결된 EBS 볼륨과 보안 그룹 삭제. GitHub 저장소와 README는 보존.
