package com.coupon.rush.coupon.issued.application;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.coupon.issued.application.port.out.IssueDecision;
import com.coupon.rush.coupon.issued.application.port.out.IssueRoster;
import com.coupon.rush.coupon.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.coupon.issued.domain.IssuedCoupon;
import com.coupon.rush.coupon.issued.domain.Status;
import com.coupon.rush.coupon.round.application.port.out.IssueRoundRepository;
import com.coupon.rush.coupon.round.domain.IssueRound;
import com.coupon.rush.user.application.port.out.UserRepository;
import com.coupon.rush.user.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssuedCouponServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private static final Long ROUND_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private FakeIssuedCouponRepository issuedCouponRepository;
    private FakeIssueRoundRepository issueRoundRepository;
    private FakeUserRepository userRepository;
    private FakeIssueRoster issueRoster;
    private IssuedCouponService service;

    @BeforeEach
    void setUp() {
        issuedCouponRepository = new FakeIssuedCouponRepository();
        issueRoundRepository = new FakeIssueRoundRepository();
        userRepository = new FakeUserRepository();
        userRepository.ids.add(USER_ID);
        userRepository.ids.add(OTHER_USER_ID);
        issueRoster = new FakeIssueRoster();
        service = new IssuedCouponService(issuedCouponRepository, issueRoundRepository, userRepository, issueRoster, CLOCK);
        // 서비스가 트랜잭션 종료 콜백을 등록하므로, 스프링 없이도 등록할 수 있게 동기화를 켠다
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void issue_성공() {
        issueRoundRepository.put(ROUND_ID, openRound(100));

        Long id = service.issue(ROUND_ID, USER_ID);

        IssuedCoupon saved = issuedCouponRepository.findById(id).orElseThrow();
        assertThat(saved.getRoundId()).isEqualTo(ROUND_ID);
        assertThat(saved.getUsersId()).isEqualTo(USER_ID);
        assertThat(saved.getStatus()).isEqualTo(Status.ISSUED);
    }

    @Test
    void issue_회차가_없으면_ROUND_NOT_FOUND() {
        assertErrorCode(() -> service.issue(ROUND_ID, USER_ID), ErrorCode.ROUND_NOT_FOUND);
    }

    @Test
    void issue_사용자가_없으면_USER_NOT_FOUND() {
        issueRoundRepository.put(ROUND_ID, openRound(100));

        assertErrorCode(() -> service.issue(ROUND_ID, 999L), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void issue_오픈_전이면_ROUND_NOT_OPEN() {
        issueRoundRepository.put(ROUND_ID, IssueRound.create(1L, NOW.plusMinutes(1), NOW.plusHours(1), 100));

        assertErrorCode(() -> service.issue(ROUND_ID, USER_ID), ErrorCode.ROUND_NOT_OPEN);
    }

    @Test
    void issue_마감_시각이_지났으면_ROUND_CLOSED() {
        issueRoundRepository.put(ROUND_ID, IssueRound.create(1L, NOW.minusHours(1), NOW, 100));

        assertErrorCode(() -> service.issue(ROUND_ID, USER_ID), ErrorCode.ROUND_CLOSED);
    }

    @Test
    void issue_같은_회차를_다시_받으면_ALREADY_ISSUED() {
        issueRoundRepository.put(ROUND_ID, openRound(100));
        service.issue(ROUND_ID, USER_ID);

        assertErrorCode(() -> service.issue(ROUND_ID, USER_ID), ErrorCode.ALREADY_ISSUED);
    }

    @Test
    void issue_수량이_소진되면_SOLD_OUT() {
        issueRoundRepository.put(ROUND_ID, openRound(1));
        service.issue(ROUND_ID, USER_ID);

        assertErrorCode(() -> service.issue(ROUND_ID, OTHER_USER_ID), ErrorCode.SOLD_OUT);
    }

    @Test
    void DB_저장이_롤백되면_발급_명단에서도_뺀다() {
        issueRoundRepository.put(ROUND_ID, openRound(100));
        service.issue(ROUND_ID, USER_ID);

        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        assertThat(issueRoster.contains(ROUND_ID, USER_ID)).isFalse();
    }

    @Test
    void DB_저장이_커밋되면_발급_명단에_남는다() {
        issueRoundRepository.put(ROUND_ID, openRound(100));
        service.issue(ROUND_ID, USER_ID);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        assertThat(issueRoster.contains(ROUND_ID, USER_ID)).isTrue();
    }

    @Test
    void 거절된_요청은_명단_되돌리기를_등록하지_않는다() {
        issueRoundRepository.put(ROUND_ID, openRound(1));
        service.issue(ROUND_ID, USER_ID);
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.initSynchronization();

        assertErrorCode(() -> service.issue(ROUND_ID, OTHER_USER_ID), ErrorCode.SOLD_OUT);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    // 실제로는 스프링 트랜잭션 매니저가 커밋/롤백 뒤에 호출하는 부분을 직접 흉내 낸다
    private static void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(status));
    }

    private static IssueRound openRound(int totalQuantity) {
        return IssueRound.create(1L, NOW.minusHours(1), NOW.plusHours(1), totalQuantity);
    }

    private static void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }

    private static class FakeIssuedCouponRepository implements IssuedCouponRepository {
        private final List<IssuedCoupon> store = new ArrayList<>();
        private long sequence = 0;

        @Override
        public IssuedCoupon save(IssuedCoupon issuedCoupon) {
            // id 세터가 없어 DB 의 IDENTITY 채번을 리플렉션으로 흉내 낸다.
            ReflectionTestUtils.setField(issuedCoupon, "id", ++sequence);
            store.add(issuedCoupon);
            return issuedCoupon;
        }

        @Override
        public Optional<IssuedCoupon> findById(Long id) {
            return store.stream().filter(c -> c.isIdEqualTo(id)).findFirst();
        }

        @Override
        public List<IssuedCoupon> findAllByUsersId(Long usersId) {
            return store.stream().filter(c -> c.getUsersId().equals(usersId)).toList();
        }

        @Override
        public boolean existsByRoundIdAndUsersId(Long roundId, Long usersId) {
            return store.stream().anyMatch(c -> c.getRoundId().equals(roundId) && c.getUsersId().equals(usersId));
        }

        @Override
        public boolean existsByRoundId(Long roundId) {
            return store.stream().anyMatch(c -> c.getRoundId().equals(roundId));
        }

        @Override
        public long countByRoundId(Long roundId) {
            return store.stream().filter(c -> c.getRoundId().equals(roundId)).count();
        }
    }

    private static class FakeIssueRoundRepository implements IssueRoundRepository {
        private final Map<Long, IssueRound> store = new HashMap<>();

        void put(Long id, IssueRound round) {
            store.put(id, round);
        }

        @Override
        public IssueRound save(IssueRound round) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<IssueRound> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<IssueRound> findAllByPolicyId(Long policyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsByPolicyId(Long policyId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(IssueRound round) {
            throw new UnsupportedOperationException();
        }
    }

    // Redis 명단과 같은 판정 규칙을 메모리로 흉내 낸다
    private static class FakeIssueRoster implements IssueRoster {
        private final Map<Long, Set<Long>> rosters = new HashMap<>();

        @Override
        public IssueDecision tryIssue(Long roundId, Long usersId, int totalQuantity) {
            Set<Long> roster = rosters.computeIfAbsent(roundId, id -> new HashSet<>());
            if (roster.contains(usersId)) {
                return IssueDecision.DUPLICATED;
            }
            if (roster.size() >= totalQuantity) {
                return IssueDecision.SOLD_OUT;
            }
            roster.add(usersId);
            return IssueDecision.ACCEPTED;
        }

        @Override
        public void cancel(Long roundId, Long usersId) {
            rosters.getOrDefault(roundId, new HashSet<>()).remove(usersId);
        }

        boolean contains(Long roundId, Long usersId) {
            return rosters.getOrDefault(roundId, Set.of()).contains(usersId);
        }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Set<Long> ids = new HashSet<>();

        @Override
        public User save(User user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<User> findById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsById(Long id) {
            return ids.contains(id);
        }

        @Override
        public boolean existsByUserId(String userId) {
            throw new UnsupportedOperationException();
        }
    }
}
