#!/usr/bin/env bash
# infra 서버에 MySQL, Redis 를 띄운다.  사용법: deploy/infra-up.sh [--reset]
#   --reset : 기존 데이터를 지우고 빈 DB 로 다시 띄운다 (단계별 측정 전에 조건을 맞출 때)
# 포트는 열지만 보안 그룹이 같은 그룹(app, k6) 안에서만 접근을 허용한다.
source "$(dirname "$0")/lib.sh"

INFRA=$(ip_of "$INFRA_NAME" PublicIpAddress)
RESET=${1:-}

remote "$INFRA" "
    set -e
    docker() { sudo docker \"\$@\"; }   # ec2-user 가 docker 그룹이 아니라 sudo 로 실행한다
    if [[ '$RESET' == '--reset' ]]; then
        docker rm -f fcfs-mysql fcfs-redis 2> /dev/null || true
        docker volume rm fcfs-mysql-data fcfs-redis-data 2> /dev/null || true
    fi

    # 이전 실험(coupon 프로젝트)의 컨테이너가 같은 포트를 잡고 있으면 멈춘다. 지우지는 않는다.
    for c in \$(docker ps --format '{{.Names}}' | grep -v '^fcfs-' || true); do docker stop \$c > /dev/null; echo \"stopped \$c\"; done

    docker start fcfs-mysql 2> /dev/null || docker run -d --name fcfs-mysql -p 3306:3306 \
        -e MYSQL_DATABASE=fcfs -e MYSQL_USER=fcfs -e MYSQL_PASSWORD=fcfs1234 -e MYSQL_ROOT_PASSWORD=root1234 -e TZ=Asia/Seoul \
        -v fcfs-mysql-data:/var/lib/mysql \
        mysql:8.4 \
        --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci \
        --innodb-buffer-pool-size=1G --max-connections=500

    docker start fcfs-redis 2> /dev/null || docker run -d --name fcfs-redis -p 6379:6379 \
        -v fcfs-redis-data:/data \
        redis:7-alpine redis-server --appendonly yes --appendfsync everysec --maxmemory-policy noeviction

    until docker exec fcfs-mysql mysqladmin ping -h localhost -ufcfs -pfcfs1234 --silent 2> /dev/null; do sleep 2; done
    docker ps --format '{{.Names}} {{.Status}}'
"
