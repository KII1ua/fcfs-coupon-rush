package com.coupon.rush.coupon.issued.adapter.out.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

// MULTI 를 열고 EXEC 로 닫는 틀. 어떤 명령을 넣을지는 RedisTransactionalOperation 이 정한다.
// SessionCallback 안의 명령은 모두 같은 커넥션에서 실행된다 (MULTI~EXEC 는 한 커넥션이어야 한다).
@Component
@RequiredArgsConstructor
class RedisTransaction {

    private final StringRedisTemplate redisTemplate;

    <R> R execute(RedisTransactionalOperation<R> operation) {
        List<Object> results = redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                operation.queue(operations);
                return operations.exec();
            }
        });
        return operation.interpret(results);
    }
}
