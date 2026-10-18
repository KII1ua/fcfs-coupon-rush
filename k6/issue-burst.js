import exec from 'k6/execution';
import {
    createOpenRound, createUsers, createWarmup, issue, measureStartTime, printVerifyGuide, warmupIssue, warmupScenarios,
} from './lib/fixture.js';

// ── 시나리오 1. 서로 다른 유저가 오픈 순간에 한꺼번에 몰린다 (초과 발급 확인) ──
//   k6 run k6/issue-burst.js
//   k6 run -e USERS=2000 -e QUANTITY=100 k6/issue-burst.js
//
//   USERS    : 동시에 요청하는 서로 다른 유저 수 = VU 수 (기본 1000)
//   QUANTITY : 회차 발급 수량 (기본 100)
//   BASE_URL : 서버 주소 (기본 http://localhost:8080)
//   WARMUP_SECONDS : 본 측정 전 예열 시간 (기본 10초, 0 이면 예열 없음. lib/fixture.js 참고)
//
// VU 하나가 유저 한 명이고, 모든 VU 가 동시에 딱 한 번 발급을 요청한다.
// 발급 성공(coupon_issued)이 QUANTITY 를 넘으면 threshold 가 실패한다 = 초과 발급.

const USERS = parseInt(__ENV.USERS || '1000');
const QUANTITY = parseInt(__ENV.QUANTITY || '100');

export const options = {
    setupTimeout: '5m',
    scenarios: {
        ...warmupScenarios(),
        burst: {
            startTime: measureStartTime(),
            executor: 'per-vu-iterations',
            vus: USERS,
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        coupon_issued: [`count<=${QUANTITY}`],
        coupon_unexpected: ['count==0'],
        // 정합성 시나리오라 응답 시간 기준은 두지 않는다. 항상 통과하는 조건으로 발급 요청만의 응답 시간을 요약에 띄운다
        'http_req_duration{name:issue}': ['max>=0'],
        'http_reqs{name:issue}': ['count>0'],
    },
};

export function setup() {
    return { roundId: createOpenRound(QUANTITY), userIds: createUsers(USERS), warmup: createWarmup() };
}

export function warmup(data) {
    warmupIssue(data.warmup, exec.scenario.iterationInTest);
}

export default function (data) {
    // VU 번호(__VU)는 예열 시나리오의 VU 까지 포함해 매겨지므로, 이 시나리오 안에서의 순번으로 유저를 고른다 (0 ~ USERS-1)
    issue(data.roundId, data.userIds[exec.scenario.iterationInTest]);
}

export function teardown(data) {
    printVerifyGuide(data.roundId);
}
