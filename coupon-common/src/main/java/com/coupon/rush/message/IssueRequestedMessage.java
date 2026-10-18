package com.coupon.rush.message;

import java.time.Instant;

// 발급 서버가 Redis 판정을 통과한 요청을 처리 서버로 넘기는 메시지. 두 서버 사이의 계약이라 공통 모듈에 둔다.
// requestedAt 은 발급 서버가 판정한 시각이다. 처리 서버가 늦게 저장하더라도 발급 시각은 이 값을 기준으로 한다.
public record IssueRequestedMessage(Long roundId, Long usersId, Instant requestedAt) {

    public static final String TOPIC = "coupon-issue-requested";

    // 같은 유저의 메시지는 같은 파티션으로 가서 순서대로 처리된다. 회차 id 를 키로 쓰면 한 회차가 파티션 하나에 몰린다
    public String key() {
        return String.valueOf(usersId);
    }
}
