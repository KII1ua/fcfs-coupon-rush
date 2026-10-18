#!/usr/bin/env bash
# k6 서버에서 시나리오를 실행하고, 끝나면 infra 서버의 DB 로 정합성을 확인한 뒤 증빙 자료를 남긴다.
#   사용법: deploy/k6-run.sh <시나리오 파일> [k6 옵션...]
#   예)    deploy/k6-run.sh issue-burst.js -e USERS=2000 -e QUANTITY=100
#          deploy/k6-run.sh issue-throughput.js -e RATE=1000 -e DURATION=30s
#   단계 이름을 남기려면: STAGE=pessimistic-lock deploy/k6-run.sh ...  (결과 폴더 이름과 캡처 머리말에 들어간다)
#   결과:  deploy/results/<시각>-<시나리오>[-<단계>]/ 아래에
#          summary.txt / summary.png (k6 요약 + DB 검증), report.html / report.png (k6 웹 대시보드)
source "$(dirname "$0")/lib.sh"

SCENARIO=${1:?시나리오 파일 이름을 넣어 주세요 (예: issue-burst.js)}; shift
K6=$(ip_of "$K6_NAME" PublicIpAddress)
INFRA=$(ip_of "$INFRA_NAME" PublicIpAddress)
APP_PRIVATE=$(ip_of "$APP_NAME" PrivateIpAddress)

type_of() {
    aws ec2 describe-instances --region "$REGION" --filters "Name=tag:Name,Values=$1" "Name=instance-state-name,Values=running" \
        --query "Reservations[0].Instances[0].InstanceType" --output text
}

STAGE=${STAGE:-}
OUT="$ROOT/deploy/results/$(date +%Y%m%d-%H%M%S)-${SCENARIO%.js}${STAGE:+-$STAGE}"
mkdir -p "$OUT"
LOG="$OUT/summary.txt"

COMMIT=$(git -C "$ROOT" rev-parse --short HEAD)
git -C "$ROOT" diff --quiet HEAD -- src || COMMIT="$COMMIT (커밋 안 된 변경 있음)"
{
    echo "# 시각     : $(date '+%Y-%m-%d %H:%M:%S %Z')"
    [[ -n "$STAGE" ]] && echo "# 단계     : $STAGE"
    echo "# 시나리오 : $SCENARIO $*"
    echo "# 코드     : $COMMIT"
    echo "# 서버     : k6 $(type_of "$K6_NAME") / app $(type_of "$APP_NAME") / infra $(type_of "$INFRA_NAME") (MySQL 8.4, Redis 7), AWS 서울 같은 AZ"
    echo
} > "$LOG"

remote "$K6" "rm -rf ~/k6"
upload "$K6" "$ROOT/k6" "~/k6"

# 같은 VPC 안에서 사설 IP 로 요청한다. 공인 IP 를 거치면 인터넷 구간 지연이 측정값에 섞인다.
remote "$K6" "cd ~/k6 && K6_WEB_DASHBOARD=true K6_WEB_DASHBOARD_PERIOD=1s K6_WEB_DASHBOARD_EXPORT=report.html \
    k6 run --no-color -e BASE_URL=http://$APP_PRIVATE:8080 $* $SCENARIO" 2>&1 | tee -a "$LOG" || true
scp -i "$KEY" -o LogLevel=ERROR "$SSH_USER@$K6:~/k6/report.html" "$OUT/report.html" 2> /dev/null || echo "(k6 리포트 없음)"

python3 "$ROOT/deploy/throughput.py" "$LOG" | tee -a "$LOG"

ROUND_ID=$(grep -o '회차 id = [0-9]*' "$LOG" | grep -o '[0-9]*$' || true)
if [[ -n "$ROUND_ID" ]]; then
    echo "== DB 검증 (회차 $ROUND_ID)" | tee -a "$LOG"
    remote "$INFRA" "sudo docker exec -i fcfs-mysql mysql -ufcfs -pfcfs1234 fcfs -t --init-command='set @round_id=$ROUND_ID'" \
        < "$ROOT/k6/verify.sql" 2> /dev/null | tee -a "$LOG"
fi

python3 "$ROOT/deploy/capture.py" "$OUT"
echo "결과 폴더: $OUT"
