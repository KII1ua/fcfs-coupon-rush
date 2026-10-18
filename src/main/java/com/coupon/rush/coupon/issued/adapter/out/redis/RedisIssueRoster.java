package com.coupon.rush.coupon.issued.adapter.out.redis;

import com.coupon.rush.coupon.issued.application.port.out.IssueDecision;
import com.coupon.rush.coupon.issued.application.port.out.IssueRoster;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class RedisIssueRoster implements IssueRoster {

    // 처음 한 번만 본문(EVAL)을 보내고, 이후에는 해시(EVALSHA)로 실행한다
    private static final RedisScript<Long> ISSUE_SCRIPT =
            RedisScript.of(new ClassPathResource("redis/issue.lua"), Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public IssueDecision tryIssue(Long roundId, Long usersId, int totalQuantity) {
        Long result = redisTemplate.execute(ISSUE_SCRIPT, List.of(key(roundId)),
                usersId.toString(), String.valueOf(totalQuantity));

        return switch (result.intValue()) {
            case 0 -> IssueDecision.ACCEPTED;
            case 1 -> IssueDecision.DUPLICATED;
            case 2 -> IssueDecision.SOLD_OUT;
            default -> throw new IllegalStateException("알 수 없는 발급 판정 결과: " + result);
        };
    }

    @Override
    public void cancel(Long roundId, Long usersId) {
        redisTemplate.opsForSet().remove(key(roundId), usersId.toString());
    }

    private String key(Long roundId) {
        return "fcfs:round:" + roundId + ":issued-users";
    }
}
