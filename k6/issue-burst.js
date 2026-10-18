import { createOpenRound, createUsers, issue, printVerifyGuide } from './lib/fixture.js';

// ── 시나리오 1. 서로 다른 유저가 오픈 순간에 한꺼번에 몰린다 (초과 발급 확인) ──
//   k6 run k6/issue-burst.js
//   k6 run -e USERS=2000 -e QUANTITY=100 k6/issue-burst.js
//
//   USERS    : 동시에 요청하는 서로 다른 유저 수 = VU 수 (기본 1000)
//   QUANTITY : 회차 발급 수량 (기본 100)
//   BASE_URL : 서버 주소 (기본 http://localhost:8080)
//
// VU 하나가 유저 한 명이고, 모든 VU 가 동시에 딱 한 번 발급을 요청한다.
// 발급 성공(coupon_issued)이 QUANTITY 를 넘으면 threshold 가 실패한다 = 초과 발급.

const USERS = parseInt(__ENV.USERS || '1000');
const QUANTITY = parseInt(__ENV.QUANTITY || '100');

export const options = {
    setupTimeout: '5m',
    scenarios: {
        burst: {
            executor: 'per-vu-iterations',
            vus: USERS,
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        coupon_issued: [`count<=${QUANTITY}`],
        coupon_unexpected: ['count==0'],
        'http_req_duration{name:issue}': ['p(95)<1000'],
        'http_reqs{name:issue}': ['count>0'],
    },
};

export function setup() {
    return { roundId: createOpenRound(QUANTITY), userIds: createUsers(USERS) };
}

export default function (data) {
    issue(data.roundId, data.userIds[__VU - 1]);
}

export function teardown(data) {
    printVerifyGuide(data.roundId);
}
