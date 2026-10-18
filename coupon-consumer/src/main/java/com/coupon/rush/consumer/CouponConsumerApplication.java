package com.coupon.rush.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CouponConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponConsumerApplication.class, args);
    }
}
