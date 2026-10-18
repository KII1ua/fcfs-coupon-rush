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
    WARMUP=$(echo "$*" | grep -oE 'WARMUP_SECONDS=[0-9]+' | cut -d= -f2)
    WARMUP=${WARMUP:-10}
    if [[ "$WARMUP" == "0" ]]; then echo "# 예열     : 없음 (배포 직후 상태)"; else echo "# 예열     : ${WARMUP}초, 별도 회차로 초당 50건 (집계 제외)"; fi
    echo "# 코드     : $COMMIT"
    CONSUMER_TYPE=$(type_of "$CONSUMER_NAME")
    if [[ "$CONSUMER_TYPE" != "None" ]]; then
        echo "# 서버     : k6 $(type_of "$K6_NAME") / api $(type_of "$APP_NAME") / consumer $CONSUMER_TYPE / infra $(type_of "$INFRA_NAME") (MySQL 8.4, Redis 7, Kafka 3.8), AWS 서울 같은 AZ"
    else
        echo "# 서버     : k6 $(type_of "$K6_NAME") / app $(type_of "$APP_NAME") / infra $(type_of "$INFRA_NAME") (MySQL 8.4, Redis 7), AWS 서울 같은 AZ"
    fi
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
    # 비동기 저장 단계에서는 k6 가 끝나도 처리 서버가 아직 저장 중일 수 있다.
    # 회차의 발급 기록 수가 3초 연속 그대로면 저장이 끝난 것으로 보고, 끝나기까지 걸린 시간을 남긴다 (최대 120초)
    COUNT_SQL="select count(*) from issued_coupon where round_id = $ROUND_ID"
    WAIT=$(remote "$INFRA" "
        prev=-1; stable=0; start=\$(date +%s)
        for i in \$(seq 1 120); do
            now=\$(sudo docker exec fcfs-mysql mysql -ufcfs -pfcfs1234 fcfs -N -e '$COUNT_SQL' 2> /dev/null)
            if [[ \"\$now\" == \"\$prev\" ]]; then stable=\$((stable + 1)); else stable=0; fi
            prev=\$now
            [[ \$stable -ge 3 ]] && break
            sleep 1
        done
        echo \"\$prev \$(( \$(date +%s) - start - 3 ))\"
    ")
    echo "== 저장 완료 대기: DB 발급 기록 ${WAIT% *}건, k6 종료 후 약 ${WAIT#* }초 뒤 더 늘지 않음" | tee -a "$LOG"
    remote "$INFRA" "sudo docker exec fcfs-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group coupon-issue-consumer 2> /dev/null" \
        | awk 'NF >= 6 && $1 != "GROUP" {lag += $6} END {if (NR > 0) print "== Kafka 컨슈머 지연(lag) 합계: " lag + 0 "건"}' | tee -a "$LOG"

    echo "== DB 검증 (회차 $ROUND_ID)" | tee -a "$LOG"
    remote "$INFRA" "sudo docker exec -i fcfs-mysql mysql -ufcfs -pfcfs1234 fcfs -t --init-command='set @round_id=$ROUND_ID'" \
        < "$ROOT/k6/verify.sql" 2> /dev/null | tee -a "$LOG"
fi

python3 "$ROOT/deploy/capture.py" "$OUT"
echo "결과 폴더: $OUT"
