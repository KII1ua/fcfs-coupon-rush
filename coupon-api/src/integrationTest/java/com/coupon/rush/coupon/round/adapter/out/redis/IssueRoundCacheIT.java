package com.coupon.rush.coupon.round.adapter.out.redis;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import com.coupon.rush.coupon.round.application.IssueRoundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 회차를 만들면 발급 판정에 쓸 회차 정보가 Redis 에도 저장되는지 확인한다
class IssueRoundCacheIT extends IntegrationTestSupport {

    @Autowired
    private IssueRoundService issueRoundService;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void 회차를_만들면_수량과_오픈_마감_시각이_Redis_에_저장된다() {
        LocalDateTime openedAt = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime closedAt = LocalDateTime.of(2026, 11, 1, 12, 0, 30);
        Long policyId = couponPolicyRepository.save(
                CouponPolicy.create("캐시 테스트 정책", 1000, 1000, closedAt.plusDays(30))).getId();

        Long roundId = issueRoundService.create(policyId, openedAt, closedAt, 100);

        Map<Object, Object> meta = redisTemplate.opsForHash().entries("fcfs:round:" + roundId + ":meta");
        assertThat(meta).containsEntry("totalQuantity", "100");
        assertThat(LocalDateTime.parse((String) meta.get("openedAt"))).isEqualTo(openedAt);
        assertThat(LocalDateTime.parse((String) meta.get("closedAt"))).isEqualTo(closedAt);
    }

    @Test
    void 회차_정보는_마감_뒤에_만료된다() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime closedAt = now.plusHours(2);
        Long policyId = couponPolicyRepository.save(
                CouponPolicy.create("만료 테스트 정책", 1000, 1000, now.plusDays(30))).getId();

        Long roundId = issueRoundService.create(policyId, now.minusHours(1), closedAt, 10);

        Long ttlSeconds = redisTemplate.getExpire("fcfs:round:" + roundId + ":meta", TimeUnit.SECONDS);
        long untilClose = Duration.between(now, closedAt.atZone(ZoneId.systemDefault()).toLocalDateTime()).toSeconds();
        assertThat(ttlSeconds).isGreaterThan(untilClose);   // 마감 전에 사라지면 판정 정보가 없어진다
    }
}
