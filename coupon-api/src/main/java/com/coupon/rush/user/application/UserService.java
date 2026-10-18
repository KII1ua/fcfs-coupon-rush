package com.coupon.rush.user.application;

import com.coupon.rush._config.exception.BusinessException;
import com.coupon.rush._config.exception.ErrorCode;
import com.coupon.rush.user.application.port.out.UserRepository;
import com.coupon.rush.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public Long register(String userId, String password) {
        if (userRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
        }
        return userRepository.save(User.from(userId, password)).getId();
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
