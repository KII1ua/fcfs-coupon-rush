import { createOpenRound, createUsers, issue, printVerifyGuide } from './lib/fixture.js';

// ── 시나리오 2. 같은 유저가 발급 버튼을 연타한다 (1인 1장 확인) ──
//   k6 run k6/issue-duplicate.js
//   k6 run -e USERS=50 -e REPEAT=20 k6/issue-duplicate.js
//
//   USERS  : 유저 수 (기본 100)
//   REPEAT : 유저 한 명이 동시에 보내는 요청 수 (기본 10)  → VU 수 = USERS × REPEAT
//
// 수량은 유저 수보다 넉넉하게 잡아서 SOLD_OUT 이 끼지 않게 한다.
// 그러면 정상일 때 발급 성공은 정확히 USERS 건이어야 하고, 넘으면 같은 유저에게 두 장 이상 나간 것이다.

const USERS = parseInt(__ENV.USERS || '100');
const REPEAT = parseInt(__ENV.REPEAT || '10');

export const options = {
    setupTimeout: '5m',
    scenarios: {
        duplicate: {
            executor: 'per-vu-iterations',
            vus: USERS * REPEAT,
            iterations: 1,
            maxDuration: '2m',
        },
    },
    thresholds: {
        coupon_issued: [`count<=${USERS}`],
        coupon_unexpected: ['count==0'],
        // 정합성 시나리오라 응답 시간 기준은 두지 않는다. 항상 통과하는 조건으로 발급 요청만의 응답 시간을 요약에 띄운다
        'http_req_duration{name:issue}': ['max>=0'],
        'http_reqs{name:issue}': ['count>0'],
    },
};

export function setup() {
    return { roundId: createOpenRound(USERS * 10), userIds: createUsers(USERS) };
}

export default function (data) {
    // VU 1~REPEAT 는 유저 0, 그다음 REPEAT 개는 유저 1 ... 같은 유저의 요청들이 동시에 출발한다
    issue(data.roundId, data.userIds[Math.floor((__VU - 1) / REPEAT)]);
}

export function teardown(data) {
    printVerifyGuide(data.roundId);
}
