#!/usr/bin/env bash
# [5단계] 동시성 시험: 같은 순간에 신청 요청 여러 건을 보내 정원만큼만 성공하는지 확인
#
# 사용법
#   bash scripts/concurrency-test.sh [서버주소] [프로그램번호] [요청수]
# 예시
#   bash scripts/concurrency-test.sh http://localhost 5 100
#   bash scripts/concurrency-test.sh http://<EC2-퍼블릭-IP> 5 100
#
# 기대 결과 (정원 30명인 5번 프로그램에 100건)
#   201(성공) 30건 / 409(정원 마감) 70건
#
# 다시 시험하려면 DB와 Redis를 초기화: docker compose down -v && docker compose up -d --build

BASE_URL=${1:-http://localhost}
PROGRAM_ID=${2:-5}
COUNT=${3:-100}

RESULT_FILE=$(mktemp)

echo "▶ ${BASE_URL} 의 ${PROGRAM_ID}번 프로그램에 동시 신청 ${COUNT}건을 보냅니다..."

# 1단계: 학번 20260001 ~ 2026xxxx 로 요청을 동시에(-P) 보내고 응답 코드만 기록
seq 1 "$COUNT" | xargs -P "$COUNT" -I{} sh -c '
  NUM=$(printf "%04d" "$3")
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST "$1/api/programs/$2/registrations" \
    -H "Content-Type: application/json" \
    -d "{\"studentId\":\"2026${NUM}\",\"studentName\":\"시험${NUM}\"}"
' _ "$BASE_URL" "$PROGRAM_ID" {} >> "$RESULT_FILE"

# 2단계: 응답 코드별 개수 세기
SUCCESS=$(grep -c '^201$' "$RESULT_FILE")
FULL=$(grep -c '^409$' "$RESULT_FILE")
TOTAL=$(wc -l < "$RESULT_FILE")
OTHER=$((TOTAL - SUCCESS - FULL))

echo ""
echo "===== 결과 ====="
echo "성공(201)       : ${SUCCESS}건"
echo "정원 마감(409)  : ${FULL}건"
echo "기타            : ${OTHER}건"
echo ""

# 3단계: 서버가 기억하는 현황도 함께 확인
echo "===== 서버의 현재 현황 ====="
curl -s "${BASE_URL}/api/programs/${PROGRAM_ID}"
echo ""

rm -f "$RESULT_FILE"
