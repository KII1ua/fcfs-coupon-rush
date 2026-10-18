package com.coupon.rush.user.adapter.out.persistence;

import com.coupon.rush.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<User, Long> {
    boolean existsByUserId(String userId);
}
