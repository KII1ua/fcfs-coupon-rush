package com.coupon.rush.consumer._config;

import com.coupon.rush.consumer.issued.adapter.in.kafka.InvalidIssueMessageException;
import com.coupon.rush.message.IssueRequestedMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * 처리 서버의 Kafka 동작 설정은 전부 이 클래스에 있다.
 * application.yaml 에는 환경마다 달라지는 접속 정보(bootstrap-servers, group-id)만 둔다.
 * 브로커 주소는 KafkaConnectionDetails 에서 읽는다. 운영에서는 yaml 값이, 테스트에서는 Testcontainers 가 넣은 값이 들어온다.
 */
@Configuration
class KafkaConsumerConfig {

    public static final String DLT_TOPIC = IssueRequestedMessage.TOPIC + "-dlt";

    // coupon-api 가 만든 토픽의 파티션 수와 같아야 한다. 컨슈머 스레드 수와 DLT 파티션 수도 이 값을 따른다
    private static final int PARTITIONS = 3;

    // 한 번에 가져오는 양 = 컨슈머가 죽었을 때 다시 처리될 수 있는 최대 양.
    // 이전 실험(coupon 프로젝트, EC2): 500 → 50 으로 줄이자 강제 종료 시 재전달 평균 655 → 81건, 처리 속도 차이 없음
    private static final int MAX_POLL_RECORDS = 50;

    // DB 가 잠깐 끊긴 경우를 넘기려고 1초 간격으로 2번 더 시도한다. 그래도 실패하면 DLT 로 보내고 다음 메시지로 넘어간다
    private static final long RETRY_INTERVAL_MS = 1_000;
    private static final long MAX_RETRIES = 2;

    private final KafkaConnectionDetails connectionDetails;
    private final String groupId;

    KafkaConsumerConfig(KafkaConnectionDetails connectionDetails,
                        @Value("${spring.kafka.consumer.group-id}") String groupId) {
        this.connectionDetails = connectionDetails;
        this.groupId = groupId;
    }

    // ── 읽기: 어디서부터, 몇 건씩 읽는가 ─────────────────────────────────

    @Bean
    ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getConsumer().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");   // 처음 뜰 때 이미 쌓여 있던 발급 메시지를 놓치지 않는다
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);       // 시간 기준 자동 커밋을 쓰지 않는다. 커밋은 컨테이너가 처리 후에 한다
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, MAX_POLL_RECORDS);
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false); // 토픽이 없을 때 파티션 1개짜리로 몰래 만들어지지 않게 한다
        // 값은 문자열로 받고 리스너가 직접 읽는다. 깨진 메시지를 역직렬화 단계가 아니라 리스너에서 잡아 바로 DLT 로 보낼 수 있다
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer());
    }

    // ── 처리: 몇 개의 스레드로, 언제 커밋하고, 실패하면 어떻게 하는가 ───────────

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(PARTITIONS);   // 파티션 수만큼 스레드. 더 많으면 남는 스레드는 논다
        // poll 로 받은 묶음을 다 처리한 뒤 커밋한다. 중간에 죽으면 묶음이 다시 오지만 저장이 멱등이라 안전하다
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
        handler.addNotRetryableExceptions(InvalidIssueMessageException.class);   // 다시 읽어도 결과가 같으니 바로 DLT
        return handler;
    }

    // ── DLT: 끝내 처리하지 못한 메시지를 보관하는 곳 ───────────────────────────

    @Bean
    ProducerFactory<String, String> dltProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducer().getBootstrapServers());
        props.put(ProducerConfig.ACKS_CONFIG, "all");   // DLT 로 옮긴 메시지마저 잃으면 추적할 방법이 없다
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer());
    }

    @Bean
    KafkaTemplate<String, String> dltKafkaTemplate(ProducerFactory<String, String> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    NewTopic issueRequestedDeadLetterTopic() {
        // DLT 로 보낼 때 원래 파티션 번호를 그대로 쓰므로 원본 토픽과 파티션 수를 맞춘다
        return TopicBuilder.name(DLT_TOPIC).partitions(PARTITIONS).replicas(1).build();
    }
}
