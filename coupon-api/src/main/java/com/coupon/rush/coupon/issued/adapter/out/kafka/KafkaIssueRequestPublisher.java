package com.coupon.rush.coupon.issued.adapter.out.kafka;

import com.coupon.rush.coupon.issued.application.port.out.IssueRequestPublisher;
import com.coupon.rush.message.IssueRequestedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
class KafkaIssueRequestPublisher implements IssueRequestPublisher {

    // producer 의 delivery.timeout.ms(3초)보다 조금 길게 잡아, 결과는 producer 가 먼저 확정하게 한다
    private static final long SEND_WAIT_SECONDS = 5;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    // 브로커의 ack 를 받을 때까지 기다린다. 비동기로 보내고 바로 응답하면, 전송이 실패해도 사용자는 성공 응답을 받는다
    @Override
    public void publish(IssueRequestedMessage message) {
        String payload = jsonMapper.writeValueAsString(message);
        try {
            kafkaTemplate.send(IssueRequestedMessage.TOPIC, message.key(), payload).get(SEND_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("발급 메시지 전송 중 중단됨", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("발급 메시지 전송 실패", e);
        }
    }
}
