package com.coupon.rush.coupon.round.domain;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueRoundTest {

    private static final LocalDateTime OPENED_AT = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Test
    void 정상_값으로_회차를_생성한다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.getPolicyId()).isEqualTo(1L);
        assertThat(round.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(round.getClosedAt()).isEqualTo(CLOSED_AT);
        assertThat(round.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    void 오픈_시각이_마감_시각보다_늦으면_생성에_실패한다() {
        assertThatThrownBy(() -> IssueRound.create(1L, CLOSED_AT, OPENED_AT, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 오픈_시각과_마감_시각이_같으면_생성에_실패한다() {
        assertThatThrownBy(() -> IssueRound.create(1L, OPENED_AT, OPENED_AT, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 수량이_0이면_생성에_실패한다() {
        assertThatThrownBy(() -> IssueRound.create(1L, OPENED_AT, CLOSED_AT, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 수량이_음수면_생성에_실패한다() {
        assertThatThrownBy(() -> IssueRound.create(1L, OPENED_AT, CLOSED_AT, -1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 오픈_시각이_null이면_생성에_실패한다() {
        assertThatThrownBy(() -> IssueRound.create(1L, null, CLOSED_AT, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 마감_시각이_null이면_생성에_실패한다() {
        assertThatThrownBy(() -> IssueRound.create(1L, OPENED_AT, null, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 정상_값으로_회차를_수정한다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);
        LocalDateTime newOpenedAt = OPENED_AT.plusDays(1);
        LocalDateTime newClosedAt = CLOSED_AT.plusDays(1);

        round.update(newOpenedAt, newClosedAt, 50);

        assertThat(round.getOpenedAt()).isEqualTo(newOpenedAt);
        assertThat(round.getClosedAt()).isEqualTo(newClosedAt);
        assertThat(round.getTotalQuantity()).isEqualTo(50);
    }

    @Test
    void 잘못된_값으로_수정하면_실패하고_기존_값이_유지된다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThatThrownBy(() -> round.update(CLOSED_AT, OPENED_AT, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);

        assertThat(round.getOpenedAt()).isEqualTo(OPENED_AT);
        assertThat(round.getClosedAt()).isEqualTo(CLOSED_AT);
        assertThat(round.getTotalQuantity()).isEqualTo(100);
    }

    @Test
    void null_값으로_수정하면_실패한다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThatThrownBy(() -> round.update(null, CLOSED_AT, 100))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ROUND);
    }

    @Test
    void 오픈_시각_이전이면_오픈_전이다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.isBeforeOpen(OPENED_AT.minusNanos(1))).isTrue();
    }

    @Test
    void 정확히_오픈_시각이면_열린_상태다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.isBeforeOpen(OPENED_AT)).isFalse();
        assertThat(round.isClosed(OPENED_AT)).isFalse();
    }

    @Test
    void 마감_직전까지는_마감되지_않았다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.isClosed(CLOSED_AT.minusNanos(1))).isFalse();
    }

    @Test
    void 정확히_마감_시각이면_마감이다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.isClosed(CLOSED_AT)).isTrue();
        assertThat(round.isClosed(CLOSED_AT.plusSeconds(1))).isTrue();
    }

    @Test
    void 발급_수가_총_수량보다_적으면_매진이_아니다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.isSoldOut(0)).isFalse();
        assertThat(round.isSoldOut(99)).isFalse();
    }

    @Test
    void 발급_수가_총_수량에_도달하면_매진이다() {
        IssueRound round = IssueRound.create(1L, OPENED_AT, CLOSED_AT, 100);

        assertThat(round.isSoldOut(100)).isTrue();
        assertThat(round.isSoldOut(101)).isTrue();
    }
}
