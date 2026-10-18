package com.coupon.rush.coupon.issued.adapter.out.redis;

import com.coupon.rush.coupon.issued.application.port.out.IssueDecision;
import com.coupon.rush.coupon.issued.application.port.out.IssueRoster;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RedisIssueRoster implements IssueRoster {

    private final RedisTransaction redisTransaction;
    private final StringRedisTemplate redisTemplate;

    @Override
    public IssueDecision tryIssue(Long roundId, Long usersId, int totalQuantity) {
        IssueDecision decision = redisTransaction.execute(
                new IssueRosterOperation(key(roundId), usersId.toString(), totalQuantity));
        if (decision == IssueDecision.SOLD_OUT) {
            cancel(roundId, usersId);   // 품절인데 SADD 는 이미 됐으니 되돌린다
        }
        return decision;
    }

    @Override
    public void cancel(Long roundId, Long usersId) {
        redisTemplate.opsForSet().remove(key(roundId), usersId.toString());
    }

    private String key(Long roundId) {
        return "fcfs:round:" + roundId + ":issued-users";
    }
}
