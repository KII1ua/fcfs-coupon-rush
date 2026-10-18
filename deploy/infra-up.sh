#!/usr/bin/env bash
# infra 서버에 MySQL, Redis, Kafka 를 띄운다.  사용법: deploy/infra-up.sh [--reset]
#   --reset : 기존 데이터를 지우고 빈 DB 로 다시 띄운다 (단계별 측정 전에 조건을 맞출 때)
# 포트는 열지만 보안 그룹이 같은 그룹(app, k6) 안에서만 접근을 허용한다.
source "$(dirname "$0")/lib.sh"

INFRA=$(ip_of "$INFRA_NAME" PublicIpAddress)
INFRA_PRIVATE=$(ip_of "$INFRA_NAME" PrivateIpAddress)
RESET=${1:-}

remote "$INFRA" "
    set -e
    docker() { sudo docker \"\$@\"; }   # ec2-user 가 docker 그룹이 아니라 sudo 로 실행한다
    if [[ '$RESET' == '--reset' ]]; then
        docker rm -f fcfs-mysql fcfs-redis fcfs-kafka 2> /dev/null || true
        docker volume rm fcfs-mysql-data fcfs-redis-data fcfs-kafka-data 2> /dev/null || true
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

    # KRaft 단일 브로커. 다른 서버(api, consumer)가 접속할 주소로 infra 의 사설 IP 를 광고한다
    docker start fcfs-kafka 2> /dev/null || docker run -d --name fcfs-kafka -p 9092:9092 \
        -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller \
        -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
        -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://$INFRA_PRIVATE:9092 \
        -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
        -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
        -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
        -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
        -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
        -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=false \
        -v fcfs-kafka-data:/var/lib/kafka/data \
        apache/kafka:3.8.1

    until docker exec fcfs-mysql mysqladmin ping -h localhost -ufcfs -pfcfs1234 --silent 2> /dev/null; do sleep 2; done
    until docker exec fcfs-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do sleep 2; done
    docker ps --format '{{.Names}} {{.Status}}'
"
