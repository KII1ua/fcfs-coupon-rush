#!/usr/bin/env bash
# jar 를 빌드해 발급 서버(api)와 처리 서버(consumer)에 올리고 재시작한다.  사용법: deploy/app-deploy.sh
# api 를 먼저 띄운다. Flyway 스키마와 Kafka 토픽을 api 가 만들기 때문이다.
source "$(dirname "$0")/lib.sh"

API=$(ip_of "$APP_NAME" PublicIpAddress)
CONSUMER=$(ip_of "$CONSUMER_NAME" PublicIpAddress)
INFRA_PRIVATE=$(ip_of "$INFRA_NAME" PrivateIpAddress)

(cd "$ROOT" && ./gradlew :coupon-api:bootJar :coupon-consumer:bootJar -q)

ENV="DB_URL=jdbc:mysql://$INFRA_PRIVATE:3306/fcfs REDIS_HOST=$INFRA_PRIVATE REDIS_PORT=6379 KAFKA_BOOTSTRAP_SERVERS=$INFRA_PRIVATE:9092"

# deploy <공인 IP> <모듈> <기동 확인 명령>
# 힙을 고정(Xms=Xmx)해서 측정 중에 힙 크기가 바뀌며 생기는 흔들림을 없앤다. 4GB 서버 기준.
# pkill -f 'java -jar' 는 이 명령을 실행하는 셸 자신까지 잡아 죽이므로 PID 파일로 관리한다.
deploy() {
    local host=$1 module=$2 ready=$3
    upload "$host" "$ROOT/$module/build/libs/$module-0.0.1-SNAPSHOT.jar" "~/app.jar"
    remote "$host" "
        if [[ -f ~/app.pid ]] && kill \$(cat ~/app.pid) 2> /dev/null; then
            while kill -0 \$(cat ~/app.pid) 2> /dev/null; do sleep 0.5; done
        fi
        $ENV nohup java -Xms2g -Xmx2g -jar ~/app.jar > ~/app.log 2>&1 &
        echo \$! > ~/app.pid
        for i in \$(seq 1 60); do
            $ready && echo '$module 기동 완료' && exit 0
            sleep 2
        done
        echo '$module 기동 실패' && tail -50 ~/app.log && exit 1
    "
}

deploy "$API" coupon-api "curl -sf localhost:8080/api/policies > /dev/null"
deploy "$CONSUMER" coupon-consumer "grep -q 'partitions assigned' ~/app.log"
