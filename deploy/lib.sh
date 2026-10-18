#!/usr/bin/env bash
# 배포 스크립트 공통. 서버 주소는 EC2 Name 태그로 그때그때 조회한다 (중지 후 재시작하면 공인 IP 가 바뀌기 때문).
set -euo pipefail

REGION=${REGION:-ap-northeast-2}
KEY=${KEY:-$HOME/.ssh/coupon-exp.pem}
SSH_USER=${SSH_USER:-ec2-user}

INFRA_NAME=${INFRA_NAME:-coupon-exp-infra}
APP_NAME=${APP_NAME:-coupon-exp-api}
CONSUMER_NAME=${CONSUMER_NAME:-coupon-exp-consumer}
K6_NAME=${K6_NAME:-coupon-exp-k6}

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

ip_of() {  # ip_of <Name 태그> <PublicIpAddress|PrivateIpAddress>
    aws ec2 describe-instances --region "$REGION" \
        --filters "Name=tag:Name,Values=$1" "Name=instance-state-name,Values=running" \
        --query "Reservations[0].Instances[0].$2" --output text
}

remote() {  # remote <공인 IP> <명령>
    ssh -i "$KEY" -o StrictHostKeyChecking=accept-new -o LogLevel=ERROR "$SSH_USER@$1" "$2"
}

upload() {  # upload <공인 IP> <로컬 경로...> <원격 경로>
    local host=$1; shift
    local dest=${*: -1}
    scp -i "$KEY" -o StrictHostKeyChecking=accept-new -o LogLevel=ERROR -r "${@:1:$#-1}" "$SSH_USER@$host:$dest"
}
