package com.coupon.rush.coupon.issued.adapter.out.redis;

import com.coupon.rush.coupon.issued.application.port.out.IssueDecision;
import com.coupon.rush.coupon.issued.application.port.out.IssueRoster;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class RedisIssueRoster implements IssueRoster {

    private final StringRedisTemplate redisTemplate;

    @Override
    public IssueDecision tryIssue(Long roundId, Long usersId, int totalQuantity) {
        String key = key(roundId);
        String member = usersId.toString();

        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, member))) {   // SISMEMBER: 이미 받았나
            return IssueDecision.DUPLICATED;
        }
        Long count = redisTemplate.opsForSet().size(key);                             // SCARD: 지금 몇 명인가
        if (count != null && count >= totalQuantity) {
            return IssueDecision.SOLD_OUT;
        }
        Long added = redisTemplate.opsForSet().add(key, member);                      // SADD: 명단에 넣기
        if (added == null || added == 0) {
            return IssueDecision.DUPLICATED;
        }
        return IssueDecision.ACCEPTED;
    }

    @Override
    public void cancel(Long roundId, Long usersId) {
        redisTemplate.opsForSet().remove(key(roundId), usersId.toString());
    }

    private String key(Long roundId) {
        return "fcfs:round:" + roundId + ":issued-users";
    }
}
