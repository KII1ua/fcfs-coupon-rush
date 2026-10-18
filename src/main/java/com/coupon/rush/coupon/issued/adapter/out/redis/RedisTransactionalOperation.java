package com.coupon.rush.coupon.issued.adapter.out.redis;

import org.springframework.data.redis.core.RedisOperations;

import java.util.List;

// MULTI 안에 넣을 명령과, EXEC 결과를 해석하는 방법을 한 곳에 둔다.
// 명령을 쌓는 순서와 결과를 꺼내는 순서가 한 클래스 안에 있어야, 한쪽만 바꿔서 판정이 조용히 뒤집히는 일이 없다.
interface RedisTransactionalOperation<R> {

    // MULTI 안의 명령은 큐에 쌓이기만 하고 null 을 돌려주므로 반환값을 두지 않는다
    void queue(RedisOperations<String, String> operations);

    // EXEC 결과를 queue() 에서 쌓은 순서대로 해석한다
    R interpret(List<Object> results);
}
