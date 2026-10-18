package com.coupon.rush.consumer;

import com.coupon.rush.message.IssueRequestedMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 처리 서버 통합 테스트 공통 베이스. MySQL 과 Kafka 를 한 번만 띄우고 모든 테스트 클래스가 공유한다.
 * 운영에서 스키마와 토픽은 coupon-api 가 만든다. 테스트에서는 엔티티 매핑으로 테이블을 만들고, 토픽은 아래 설정이 만든다.
 */
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(ConsumerIntegrationTestSupport.TopicConfig.class)
public abstract class ConsumerIntegrationTestSupport {

    @ServiceConnection
    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @ServiceConnection
    protected static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.1");

    static {
        Startables.deepStart(MYSQL, KAFKA).join();
    }

    @TestConfiguration
    static class TopicConfig {
        @Bean
        NewTopic issueRequestedTopic() {
            return TopicBuilder.name(IssueRequestedMessage.TOPIC).partitions(3).replicas(1).build();
        }
    }
}
