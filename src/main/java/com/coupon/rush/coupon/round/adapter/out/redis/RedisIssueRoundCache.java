package com.coupon.rush.coupon.round.adapter.out.redis;

import com.coupon.rush.coupon.round.application.port.out.IssueRoundCache;
import com.coupon.rush.coupon.round.domain.IssueRound;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

@Component
@RequiredArgsConstructor
class RedisIssueRoundCache implements IssueRoundCache {

    private static final Duration KEEP_AFTER_CLOSE = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(IssueRound round) {
        String key = "fcfs:round:" + round.getId() + ":meta";

        redisTemplate.opsForHash().putAll(key, Map.of(
                "totalQuantity", String.valueOf(round.getTotalQuantity()),
                "openedAt", round.getOpenedAt().toString(),
                "closedAt", round.getClosedAt().toString()));

        Instant expireAt = round.getClosedAt().atZone(ZoneId.systemDefault()).toInstant().plus(KEEP_AFTER_CLOSE);
        redisTemplate.expireAt(key, expireAt);
    }
}
