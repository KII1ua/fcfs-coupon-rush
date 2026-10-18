package com.coupon.rush.consumer.issued.domain;

import com.coupon.rush._config.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 처리 서버가 저장만 하는 발급 기록. 발급 서버의 IssuedCoupon 과 같은 테이블을 쓰지만,
// 사용·만료 같은 상태 변경은 발급 서버 몫이라 여기서는 저장에 필요한 것만 매핑한다.
// 스키마 주인은 coupon-api(Flyway). Unique 제약은 테스트에서 스키마를 만들 때 같은 이름으로 생기도록 적어 둔다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "issued_coupon", uniqueConstraints =
        @UniqueConstraint(name = IssuedCoupon.UNIQUE_ROUND_USERS, columnNames = {"round_id", "users_id"}))
public class IssuedCoupon extends BaseTimeEntity {

    public static final String UNIQUE_ROUND_USERS = "uk_issued_coupon_round_users";

    private Long roundId;

    private Long usersId;

    private String status;

    private IssuedCoupon(Long roundId, Long usersId) {
        this.roundId = roundId;
        this.usersId = usersId;
        this.status = "ISSUED";
    }

    public static IssuedCoupon issue(Long roundId, Long usersId) {
        return new IssuedCoupon(roundId, usersId);
    }
}
