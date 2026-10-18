package com.coupon.rush.coupon.issued.adapter.out.redis;

import com.coupon.rush.coupon.issued.application.port.out.IssueDecision;
import org.springframework.data.redis.core.RedisOperations;

import java.util.List;

// 선착순 발급 판정: SCARD → SADD 를 쌓고, 같은 순서로 꺼내 판정한다
record IssueRosterOperation(String key, String usersId, int totalQuantity)
        implements RedisTransactionalOperation<IssueDecision> {

    @Override
    public void queue(RedisOperations<String, String> operations) {
        operations.opsForSet().size(key);           // [0] 추가 전 인원
        operations.opsForSet().add(key, usersId);   // [1] 1 = 새로 추가, 0 = 이미 있음
    }

    @Override
    public IssueDecision interpret(List<Object> results) {
        long countBefore = (Long) results.get(0);
        long added = (Long) results.get(1);
        if (added == 0) {
            return IssueDecision.DUPLICATED;
        }
        if (countBefore >= totalQuantity) {
            return IssueDecision.SOLD_OUT;
        }
        return IssueDecision.ACCEPTED;
    }
}
