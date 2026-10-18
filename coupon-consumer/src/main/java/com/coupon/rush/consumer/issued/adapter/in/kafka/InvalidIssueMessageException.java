package com.coupon.rush.consumer.issued.adapter.in.kafka;

// 읽을 수 없는 메시지. 몇 번을 다시 시도해도 결과가 같으므로 재시도 없이 바로 DLT 로 보낸다
public class InvalidIssueMessageException extends RuntimeException {

    public InvalidIssueMessageException(String payload, Throwable cause) {
        super("읽을 수 없는 발급 메시지: " + payload, cause);
    }
}
