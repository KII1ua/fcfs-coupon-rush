package com.coupon.rush.coupon.issued.application.port.out;

import com.coupon.rush.message.IssueRequestedMessage;

// Redis 판정을 통과한 발급 요청을 처리 서버로 넘긴다. 브로커가 기록을 확인할 때까지 기다리고, 실패하면 예외를 던진다.
public interface IssueRequestPublisher {
    void publish(IssueRequestedMessage message);
}
