package com.coupon.rush.consumer.issued;

import com.coupon.rush.consumer.ConsumerIntegrationTestSupport;
import com.coupon.rush.message.IssueRequestedMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class IssueRequestListenerIT extends ConsumerIntegrationTestSupport {

    private static final String DLT_TOPIC = IssueRequestedMessage.TOPIC + "-dlt";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 발급_메시지를_받으면_발급_기록을_저장한다() {
        long roundId = newRoundId();

        send(new IssueRequestedMessage(roundId, 7L, Instant.now()));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(count(roundId, 7L)).isEqualTo(1));
        assertThat(jdbcTemplate.queryForObject(
                "select status from issued_coupon where round_id = ? and users_id = ?", String.class, roundId, 7L))
                .isEqualTo("ISSUED");
    }

    @Test
    void 같은_메시지가_여러_번_와도_한_건만_저장된다() {
        long roundId = newRoundId();
        IssueRequestedMessage message = new IssueRequestedMessage(roundId, 7L, Instant.now());

        send(message);
        send(message);
        send(message);
        send(new IssueRequestedMessage(roundId, 8L, Instant.now()));   // 앞의 메시지들이 다 처리됐는지 확인하는 표식

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(count(roundId, 8L)).isEqualTo(1));
        assertThat(count(roundId, 7L)).isEqualTo(1);
    }

    @Test
    void 읽을_수_없는_메시지는_재시도_없이_DLT_로_보낸다() {
        String broken = "{\"roundId\": \"not-a-number-" + UUID.randomUUID() + "\"}";

        kafkaTemplate.send(IssueRequestedMessage.TOPIC, "broken", broken);

        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of(DLT_TOPIC));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                consumer.poll(Duration.ofMillis(200)).forEach(received::add);
                assertThat(received).extracting(ConsumerRecord::value).contains(broken);
            });
        }
    }

    private void send(IssueRequestedMessage message) {
        kafkaTemplate.send(IssueRequestedMessage.TOPIC, message.key(), jsonMapper.writeValueAsString(message)).join();
    }

    private int count(long roundId, long usersId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from issued_coupon where round_id = ? and users_id = ?", Integer.class, roundId, usersId);
    }

    private KafkaConsumer<String, String> newConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer());
    }

    // 테스트끼리 같은 DB 를 쓰므로 회차 id 가 겹치지 않게 한다 (처리 서버 테스트에는 회차 테이블이 없어 FK 도 없다)
    private static long newRoundId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
