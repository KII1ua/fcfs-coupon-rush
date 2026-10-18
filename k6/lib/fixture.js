import http from 'k6/http';
import { fail } from 'k6';
import { Counter } from 'k6/metrics';

// 공통 준비 코드와 발급 요청. 시나리오 스크립트는 이 파일만 가져다 쓴다.

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// 서버는 LocalDateTime(타임존 없음)으로 오픈/마감을 판정한다.
// k6 와 서버의 타임존이 달라도 항상 "열려 있는" 회차가 되도록 아주 넓은 구간을 쓴다.
const FAR_PAST = '2000-01-01T00:00:00';
const FAR_FUTURE = '2099-12-31T23:59:59';

const issued = new Counter('coupon_issued');                 // 202 발급 확정 (기록은 처리 서버가 곧 저장). 동기 저장 단계에서는 201
const soldOut = new Counter('coupon_sold_out');              // 409 SOLD_OUT
const alreadyIssued = new Counter('coupon_already_issued');  // 409 ALREADY_ISSUED
const unexpected = new Counter('coupon_unexpected');         // 그 외 전부 (5xx, 타임아웃, 다른 에러 코드)

// 409 는 선착순에서 정상적인 거절이라 실패로 세지 않는다. http_req_failed 에는 진짜 에러만 남는다.
const ISSUE_PARAMS = {
    headers: JSON_HEADERS,
    tags: { name: 'issue' },
    responseCallback: http.expectedStatuses(201, 202, 409),
};

export function createOpenRound(quantity) {
    const policy = post('/api/policies', {
        name: `k6-${Date.now()}`,
        discountValue: 1000,
        maxDiscountAmount: 1000,
        validUntil: FAR_FUTURE,
    });
    const round = post('/api/rounds', {
        policyId: policy.json('id'),
        openedAt: FAR_PAST,
        closedAt: FAR_FUTURE,
        totalQuantity: quantity,
    });
    return round.json('id');
}

// 가입 API 로 유저를 만든다. 매 실행마다 접두사가 달라서 이전 실행의 유저와 겹치지 않는다.
export function createUsers(count) {
    const prefix = `k6-${Date.now()}`;
    const ids = [];
    for (let start = 0; start < count; start += 100) {
        const requests = [];
        for (let i = start; i < Math.min(start + 100, count); i++) {
            requests.push(['POST', `${BASE_URL}/api/users`,
                JSON.stringify({ userId: `${prefix}-${i}`, password: 'k6-password' }),
                { headers: JSON_HEADERS, tags: { name: 'setup' } }]);
        }
        for (const res of http.batch(requests)) {
            if (res.status !== 201) fail(`유저 생성 실패: ${res.status} ${res.body}`);
            ids.push(res.json('id'));
        }
    }
    return ids;
}

export function issue(roundId, usersId) {
    const res = http.post(`${BASE_URL}/api/issued-coupons`, JSON.stringify({ roundId, usersId }), ISSUE_PARAMS);

    if (res.status === 201 || res.status === 202) {
        issued.add(1);
    } else if (res.status === 409 && errorCode(res) === 'SOLD_OUT') {
        soldOut.add(1);
    } else if (res.status === 409 && errorCode(res) === 'ALREADY_ISSUED') {
        alreadyIssued.add(1);
    } else {
        unexpected.add(1);
    }
    return res;
}

// ── 예열 ──────────────────────────────────────────────────────────────
// 배포 직후에는 발급 경로가 처음 쓰일 때 준비 비용이 한꺼번에 든다 (Kafka 프로듀서 연결·메타데이터, Redis 커넥션 풀, JIT).
// 이 비용이 측정에 섞이면 단계끼리 비교가 흔들리므로, 본 측정 전에 별도 회차로 발급 경로를 먼저 데운다.
// 예열 요청은 name:warmup 태그로 보내고 발급 집계(coupon_*)에도 넣지 않는다. DB 검증도 본 측정 회차만 본다.
//   WARMUP_SECONDS : 예열 시간 (기본 10초). 0 이면 예열 없이 배포 직후 상태 그대로 잰다

export const WARMUP_SECONDS = parseInt(__ENV.WARMUP_SECONDS || '10');
const WARMUP_RATE = 50;          // 초당 예열 요청 수. 서버를 데우되 부하는 주지 않는 정도
const WARMUP_USERS = 300;
const WARMUP_QUANTITY = 200;     // 유저보다 적게 둬서 발급·품절·이미 발급(두 바퀴째) 경로를 모두 지나가게 한다
const WARMUP_GAP_SECONDS = 3;    // 예열 메시지가 처리 서버에서 저장을 마칠 틈

const WARMUP_PARAMS = {
    headers: JSON_HEADERS,
    tags: { name: 'warmup' },
    responseCallback: http.expectedStatuses(201, 202, 409),
};

export function warmupScenarios() {
    if (WARMUP_SECONDS <= 0) {
        return {};
    }
    return {
        warmup: {
            executor: 'constant-arrival-rate',
            rate: WARMUP_RATE,
            timeUnit: '1s',
            duration: `${WARMUP_SECONDS}s`,
            preAllocatedVUs: 20,
            maxVUs: 100,
            exec: 'warmup',
        },
    };
}

// 본 측정 시나리오의 시작 시각. 예열이 끝나고 잠깐 쉰 뒤에 시작한다
export function measureStartTime() {
    return WARMUP_SECONDS > 0 ? `${WARMUP_SECONDS + WARMUP_GAP_SECONDS}s` : '0s';
}

export function createWarmup() {
    if (WARMUP_SECONDS <= 0) {
        return null;
    }
    return { roundId: createOpenRound(WARMUP_QUANTITY), userIds: createUsers(WARMUP_USERS) };
}

export function warmupIssue(warmup, iteration) {
    const usersId = warmup.userIds[iteration % warmup.userIds.length];
    http.post(`${BASE_URL}/api/issued-coupons`, JSON.stringify({ roundId: warmup.roundId, usersId }), WARMUP_PARAMS);
}

export function printVerifyGuide(roundId) {
    console.log(`[검증] 회차 id = ${roundId}`);
    console.log(`[검증] docker exec -i fcfs-mysql mysql -ufcfs -pfcfs1234 fcfs -t --init-command="set @round_id=${roundId}" < k6/verify.sql`);
}

function post(path, body) {
    const res = http.post(`${BASE_URL}${path}`, JSON.stringify(body), { headers: JSON_HEADERS, tags: { name: 'setup' } });
    if (res.status !== 201) fail(`${path} 생성 실패: ${res.status} ${res.body}`);
    return res;
}

function errorCode(res) {
    try {
        return res.json('code');
    } catch (e) {
        return null;
    }
}
