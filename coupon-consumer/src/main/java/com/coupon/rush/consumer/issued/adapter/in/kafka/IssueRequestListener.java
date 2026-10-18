package com.coupon.rush.consumer.issued.adapter.in.kafka;

import com.coupon.rush.consumer.issued.application.IssuedCouponSaver;
import com.coupon.rush.message.IssueRequestedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
class IssueRequestListener {

    private final IssuedCouponSaver issuedCouponSaver;
    private final JsonMapper jsonMapper;

    // 값은 문자열로 받아 여기서 읽는다. 역직렬화 실패를 Kafka 설정이 아니라 코드에서 다뤄 바로 DLT 로 보낼 수 있다.
    // 예외를 던지면 에러 핸들러가 재시도하고, 그래도 실패하면 DLT 로 보낸다 (KafkaConsumerConfig).
    @KafkaListener(topics = IssueRequestedMessage.TOPIC)
    void onMessage(String payload) {
        IssueRequestedMessage message = read(payload);
        issuedCouponSaver.save(message.roundId(), message.usersId());
    }

    private IssueRequestedMessage read(String payload) {
        try {
            IssueRequestedMessage message = jsonMapper.readValue(payload, IssueRequestedMessage.class);
            if (message.roundId() == null || message.usersId() == null) {
                throw new InvalidIssueMessageException(payload, null);
            }
            return message;
        } catch (JacksonException e) {
            throw new InvalidIssueMessageException(payload, e);
        }
    }
}
