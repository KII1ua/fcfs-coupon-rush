package com.coupon.rush.coupon.round.application.port.out;

import com.coupon.rush.coupon.round.domain.IssueRound;

public interface IssueRoundCache {
    void save(IssueRound round);
}
