package com.coupon.rush.user.application.port.out;

import com.coupon.rush.user.domain.User;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    boolean existsById(Long id);
    boolean existsByUserId(String userId);
}
