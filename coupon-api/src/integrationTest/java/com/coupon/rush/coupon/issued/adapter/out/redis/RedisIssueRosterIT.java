package com.coupon.rush.coupon.issued.adapter.out.redis;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush.coupon.issued.application.port.out.IssueDecision;
import com.coupon.rush.coupon.issued.application.port.out.IssueRoster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// Redis 발급 명단만 따로 검증한다. DB 를 거치지 않으므로 요청이 Redis 판정에 정확히 동시에 몰린다.
class RedisIssueRosterIT extends IntegrationTestSupport {

    @Autowired
    private IssueRoster issueRoster;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void 처음_받으면_ACCEPTED_다시_받으면_DUPLICATED() {
        Long roundId = newRoundId();

        assertThat(issueRoster.tryIssue(roundId, 1L, 10)).isEqualTo(IssueDecision.ACCEPTED);
        assertThat(issueRoster.tryIssue(roundId, 1L, 10)).isEqualTo(IssueDecision.DUPLICATED);
    }

    @Test
    void 수량이_차면_SOLD_OUT_이고_명단에_남지_않는다() {
        Long roundId = newRoundId();
        issueRoster.tryIssue(roundId, 1L, 1);

        assertThat(issueRoster.tryIssue(roundId, 2L, 1)).isEqualTo(IssueDecision.SOLD_OUT);
        assertThat(redisTemplate.opsForSet().isMember(key(roundId), "2")).isFalse();
    }

    @Test
    void 취소하면_명단에서_빠지고_다시_받을_수_있다() {
        Long roundId = newRoundId();
        issueRoster.tryIssue(roundId, 1L, 1);

        issueRoster.cancel(roundId, 1L);

        assertThat(redisTemplate.opsForSet().size(key(roundId))).isZero();
        assertThat(issueRoster.tryIssue(roundId, 2L, 1)).isEqualTo(IssueDecision.ACCEPTED);
    }

    @Test
    void 서로_다른_유저가_동시에_몰려도_수량만큼만_명단에_들어간다() throws Exception {
        Long roundId = newRoundId();
        int users = 1000;
        int totalQuantity = 100;
        AtomicInteger accepted = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(64);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users);
        for (long usersId = 1; usersId <= users; usersId++) {
            long id = usersId;
            executor.submit(() -> {
                try {
                    start.await();
                    if (issueRoster.tryIssue(roundId, id, totalQuantity) == IssueDecision.ACCEPTED) {
                        accepted.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(accepted.get()).isEqualTo(totalQuantity);
        assertThat(redisTemplate.opsForSet().size(key(roundId))).isEqualTo(totalQuantity);
    }

    // DB 회차와 무관하게 테스트마다 겹치지 않는 회차 id 를 쓴다
    private static Long newRoundId() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
    }

    private static String key(Long roundId) {
        return "fcfs:round:" + roundId + ":issued-users";
    }
}
