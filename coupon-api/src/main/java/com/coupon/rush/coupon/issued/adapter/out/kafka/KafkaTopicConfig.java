package com.coupon.rush.coupon.issued.adapter.out.kafka;

import com.coupon.rush.message.IssueRequestedMessage;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// 토픽은 메시지를 보내는 발급 서버가 만든다. 처리 서버는 토픽이 없으면 기다릴 뿐 직접 만들지 않는다.
@Configuration
class KafkaTopicConfig {

    // 처리 서버의 컨슈머 스레드 수(concurrency)와 같게 둔다. 파티션보다 많은 컨슈머는 놀게 된다
    private static final int PARTITIONS = 3;

    @Bean
    NewTopic issueRequestedTopic(@Value("${coupon.kafka.replicas:1}") int replicas) {
        return TopicBuilder.name(IssueRequestedMessage.TOPIC).partitions(PARTITIONS).replicas(replicas).build();
    }
}
