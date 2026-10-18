#!/usr/bin/env bash
# jar 를 빌드해 app 서버에 올리고 재시작한다.  사용법: deploy/app-deploy.sh
source "$(dirname "$0")/lib.sh"

APP=$(ip_of "$APP_NAME" PublicIpAddress)
INFRA_PRIVATE=$(ip_of "$INFRA_NAME" PrivateIpAddress)

(cd "$ROOT" && ./gradlew bootJar -q)
JAR=$(ls "$ROOT"/build/libs/*-SNAPSHOT.jar | grep -v plain | head -1)
upload "$APP" "$JAR" "~/app.jar"

# 힙을 고정(Xms=Xmx)해서 측정 중에 힙 크기가 바뀌며 생기는 흔들림을 없앤다. t3.medium(4GB) 기준.
remote "$APP" "
    # pkill -f 'java -jar' 는 이 명령을 실행하는 셸 자신까지 잡아 죽이므로 PID 파일로 관리한다
    if [[ -f ~/app.pid ]] && kill \$(cat ~/app.pid) 2> /dev/null; then
        while kill -0 \$(cat ~/app.pid) 2> /dev/null; do sleep 0.5; done
    fi
    DB_URL=jdbc:mysql://$INFRA_PRIVATE:3306/fcfs REDIS_HOST=$INFRA_PRIVATE REDIS_PORT=6379 \
        nohup java -Xms2g -Xmx2g -jar ~/app.jar > ~/app.log 2>&1 &
    echo \$! > ~/app.pid
    for i in \$(seq 1 60); do
        curl -sf localhost:8080/api/policies > /dev/null && echo 'app 기동 완료' && exit 0
        sleep 2
    done
    echo 'app 기동 실패' && tail -50 ~/app.log && exit 1
"
