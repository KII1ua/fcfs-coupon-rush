package com.coupon.rush.consumer.issued.application;

import com.coupon.rush.consumer.issued.application.port.out.DuplicateIssueException;
import com.coupon.rush.consumer.issued.application.port.out.IssuedCouponRepository;
import com.coupon.rush.consumer.issued.domain.IssuedCoupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 발급 기록 저장. Kafka 는 같은 메시지를 두 번 이상 줄 수 있으므로(at-least-once) 몇 번 받아도 한 건만 남게 한다.
// 1차로 조회해서 거르고, 동시에 같은 기록이 들어오는 경우는 DB Unique 제약이 막는다.
// 트랜잭션을 걸지 않는다: 저장소의 save 가 자체 트랜잭션으로 실행되어야 Unique 위반을 잡고도 다음 메시지를 정상 처리할 수 있다.
@Slf4j
@Service
@RequiredArgsConstructor
public class IssuedCouponSaver {

    private final IssuedCouponRepository issuedCouponRepository;

    public void save(Long roundId, Long usersId) {
        if (issuedCouponRepository.existsByRoundIdAndUsersId(roundId, usersId)) {
            log.warn("이미 저장된 발급 메시지를 다시 받음: roundId={}, usersId={}", roundId, usersId);
            return;
        }
        try {
            issuedCouponRepository.save(IssuedCoupon.issue(roundId, usersId));
        } catch (DuplicateIssueException e) {
            log.warn("동시에 들어온 같은 발급 메시지를 Unique 제약이 막음: roundId={}, usersId={}", roundId, usersId);
        }
    }
}
