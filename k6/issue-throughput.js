import exec from 'k6/execution';
import { createOpenRound, createUsers, issue, printVerifyGuide } from './lib/fixture.js';

// ── 시나리오 3. 초당 요청 수를 고정하고 처리량과 응답 시간을 잰다 (단계별 비교용) ──
//   k6 run k6/issue-throughput.js
//   k6 run -e RATE=1000 -e DURATION=30s k6/issue-throughput.js
//
//   RATE     : 초당 보내는 발급 요청 수 (기본 500)
//   DURATION : 측정 시간 (기본 30s)
//   USERS    : 준비할 유저 수 (기본 10000)
//   QUANTITY : 회차 발급 수량 (기본 = USERS)
//
// 동시 사용자 수가 아니라 "도착률"을 고정한다. 서버가 느려져도 요청은 같은 속도로 계속 들어오므로
// CRUD → 락 → Redis → Kafka 단계를 같은 조건에서 비교할 수 있다.
// 서버가 못 따라오면 dropped_iterations 가 늘어난다. 그 지점이 그 단계의 한계 처리량이다.

const RATE = parseInt(__ENV.RATE || '500');
const DURATION = __ENV.DURATION || '30s';
const USERS = parseInt(__ENV.USERS || '10000');
const QUANTITY = parseInt(__ENV.QUANTITY || String(USERS));

export const options = {
    setupTimeout: '10m',
    scenarios: {
        throughput: {
            executor: 'constant-arrival-rate',
            rate: RATE,
            timeUnit: '1s',
            duration: DURATION,
            preAllocatedVUs: Math.min(RATE, 1000),
            maxVUs: Math.max(RATE * 2, 200),
        },
    },
    thresholds: {
        coupon_issued: [`count<=${QUANTITY}`],
        coupon_unexpected: ['count==0'],
        'http_req_duration{name:issue}': ['p(95)<500', 'p(99)<1000'],
        'http_reqs{name:issue}': ['count>0'],
    },
};

export function setup() {
    return { roundId: createOpenRound(QUANTITY), userIds: createUsers(USERS) };
}

export default function (data) {
    // 유저를 순서대로 돌려 쓴다. USERS 보다 요청이 많으면 두 바퀴째부터는 ALREADY_ISSUED 거절이 섞인다.
    const index = exec.scenario.iterationInTest % data.userIds.length;
    issue(data.roundId, data.userIds[index]);
}

export function teardown(data) {
    printVerifyGuide(data.roundId);
}
