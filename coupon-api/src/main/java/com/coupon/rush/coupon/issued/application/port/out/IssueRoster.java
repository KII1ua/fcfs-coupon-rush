package com.coupon.rush.coupon.issued.application.port.out;

public interface IssueRoster {
    IssueDecision tryIssue(Long roundId, Long usersId, int totalQuantity);
    void cancel(Long roundId, Long usersId);
}
