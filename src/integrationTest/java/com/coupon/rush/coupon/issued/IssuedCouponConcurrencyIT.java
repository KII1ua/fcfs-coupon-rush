package com.coupon.rush.coupon.issued;

import com.coupon.rush.IntegrationTestSupport;
import com.coupon.rush.coupon.issued.application.IssuedCouponService;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.policy.application.port.out.CouponPolicyRepository;
import com.coupon.rush.coupon.policy.domain.CouponPolicy;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import com.coupon.rush.user.application.port.out.UserRepository;
import com.coupon.rush.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

// 같은 회차에 발급 요청이 동시에 몰려도 수량과 1인 1장이 지켜지는지 확인한다.
class IssuedCouponConcurrencyIT extends IntegrationTestSupport {

    private static final int THREADS = 32;

    @Autowired
    private IssuedCouponService issuedCouponService;

    @Autowired
    private IssuedCouponRepository issuedCouponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private IssueRoundRepository issueRoundRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void 서로_다른_유저가_동시에_몰려도_수량만큼만_발급된다() throws Exception {
        Long roundId = saveOpenRound(10);
        List<Long> userIds = IntStream.range(0, 100).mapToObj(i -> saveUser()).toList();

        runConcurrently(userIds.size(), i -> issuedCouponService.issue(roundId, userIds.get(i)));

        assertThat(issuedCouponRepository.countByRoundId(roundId)).isEqualTo(10);
    }

    @Test
    void 같은_유저가_동시에_연타해도_한_장만_발급된다() throws Exception {
        Long roundId = saveOpenRound(100);
        Long usersId = saveUser();

        runConcurrently(50, i -> issuedCouponService.issue(roundId, usersId));

        assertThat(issuedCouponRepository.countByRoundId(roundId)).isEqualTo(1);
    }

    // 모든 작업을 준비시킨 뒤 한 번에 출발시킨다. 거절(SOLD_OUT, ALREADY_ISSUED)은 정상이므로 예외는 무시한다.
    private void runConcurrently(int count, IntConsumerWithException task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            int index = i;
            executor.submit(() -> {
                try {
                    start.await();
                    task.accept(index);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
    }

    private Long saveOpenRound(int totalQuantity) {
        LocalDateTime now = LocalDateTime.now();
        Long policyId = couponPolicyRepository.save(
                CouponPolicy.create("동시성 테스트 정책", 1000, 1000, now.plusDays(30))).getId();
        return issueRoundRepository.save(
                IssueRound.create(policyId, now.minusHours(1), now.plusHours(1), totalQuantity)).getId();
    }

    private Long saveUser() {
        return userRepository.save(User.from("concurrency-" + UUID.randomUUID(), "pw")).getId();
    }

    @FunctionalInterface
    private interface IntConsumerWithException {
        void accept(int index) throws Exception;
    }
}
