package com.coupon.rush._config.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
    DUPLICATE_USER_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),

    POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰 정책입니다."),
    INVALID_POLICY(HttpStatus.BAD_REQUEST, "쿠폰 정책 값이 올바르지 않습니다."),
    POLICY_IN_USE(HttpStatus.CONFLICT, "회차가 연결된 정책은 삭제할 수 없습니다."),

    ROUND_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발급 회차입니다."),
    INVALID_ROUND(HttpStatus.BAD_REQUEST, "발급 회차 값이 올바르지 않습니다."),
    ROUND_IN_USE(HttpStatus.CONFLICT, "발급 기록이 있는 회차는 삭제할 수 없습니다."),
    ROUND_NOT_OPEN(HttpStatus.CONFLICT, "아직 오픈되지 않은 회차입니다."),
    ROUND_CLOSED(HttpStatus.CONFLICT, "마감된 회차입니다."),
    ALREADY_ISSUED(HttpStatus.CONFLICT, "이미 발급받은 회차입니다."),
    SOLD_OUT(HttpStatus.CONFLICT, "준비된 수량이 모두 소진되었습니다."),

    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 쿠폰입니다."),
    COUPON_NOT_USABLE(HttpStatus.CONFLICT, "사용할 수 없는 쿠폰입니다.");

    private final HttpStatus status;
    private final String message;
}
