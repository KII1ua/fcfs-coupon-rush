package com.coupon.rush._config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// 오픈/마감 판정에 쓰는 현재 시각. 테스트에서 고정 시각으로 바꿔 끼울 수 있게 빈으로 둔다.
@Configuration
class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
