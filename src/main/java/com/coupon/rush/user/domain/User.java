package com.coupon.rush.user.domain;

import com.coupon.rush._config.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseTimeEntity {

    private String userId;

    private String password;

    private User(String userId, String password) {
        this.userId = userId;
        this.password = password;
    }

    public static User from(String userId, String password) {
        return new User(userId, password);
    }
}
