package com.coupon.rush.consumer.issued.application;

import com.coupon.rush.consumer.issued.application.port.out.DuplicateIssueException;
import com.coupon.rush.consumer.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.consumer.issued.domain.IssuedCoupon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class IssuedCouponSaverTest {

    private FakeIssuedCouponRepository repository;
    private IssuedCouponSaver saver;

    @BeforeEach
    void setUp() {
        repository = new FakeIssuedCouponRepository();
        saver = new IssuedCouponSaver(repository);
    }

    @Test
    void 처음_받은_메시지는_저장한다() {
        saver.save(1L, 7L);

        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void 같은_메시지를_다시_받으면_저장하지_않는다() {
        saver.save(1L, 7L);
        saver.save(1L, 7L);

        assertThat(repository.saved).hasSize(1);
    }

    @Test
    void 조회는_통과했지만_Unique_제약에_걸리면_예외_없이_넘어간다() {
        repository.raceWithAnotherConsumer = true;

        assertThatCode(() -> saver.save(1L, 7L)).doesNotThrowAnyException();
    }

    private static class FakeIssuedCouponRepository implements IssuedCouponRepository {
        private final List<IssuedCoupon> saved = new ArrayList<>();
        // 조회 직후 다른 컨슈머가 같은 기록을 먼저 저장한 상황을 흉내 낸다
        private boolean raceWithAnotherConsumer = false;

        @Override
        public boolean existsByRoundIdAndUsersId(Long roundId, Long usersId) {
            return saved.stream().anyMatch(c -> c.getRoundId().equals(roundId) && c.getUsersId().equals(usersId));
        }

        @Override
        public IssuedCoupon save(IssuedCoupon issuedCoupon) {
            if (raceWithAnotherConsumer || existsByRoundIdAndUsersId(issuedCoupon.getRoundId(), issuedCoupon.getUsersId())) {
                throw new DuplicateIssueException(issuedCoupon.getRoundId(), issuedCoupon.getUsersId());
            }
            saved.add(issuedCoupon);
            return issuedCoupon;
        }
    }
}
