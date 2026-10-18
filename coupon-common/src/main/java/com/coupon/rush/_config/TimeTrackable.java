package com.coupon.rush._config;

import java.time.Instant;

public interface TimeTrackable {
    Instant getCreatedAt();
    Instant getUpdatedAt();
}
