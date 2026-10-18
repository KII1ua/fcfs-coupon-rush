package com.coupon.rush;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FcfsCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcfsCouponApplication.class, args);
    }
}
