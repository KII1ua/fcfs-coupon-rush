package com.coupon.rush.consumer.issued.application.port.out;

public class DuplicateIssueException extends RuntimeException {

    public DuplicateIssueException(Long roundId, Long usersId) {
        super("이미 저장된 발급 기록: roundId=" + roundId + ", usersId=" + usersId);
    }
}
