package com.coupon.rush.coupon.issued.adapter.in.web;

import com.coupon.rush.coupon.issued.application.IssuedCouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/issued-coupons")
public class IssuedCouponController {
    private final IssuedCouponService issuedCouponService;

    @PostMapping
    ResponseEntity<IssueCouponResponse> issue(@Valid @RequestBody IssueCouponRequest req) {
        issuedCouponService.issue(req.roundId(), req.usersId());
        // 발급은 확정됐지만 발급 기록은 처리 서버가 곧 저장하므로 201 이 아니라 202 로 응답한다
        return ResponseEntity.accepted().body(new IssueCouponResponse(req.roundId(), req.usersId()));
    }

    @GetMapping("/{id}")
    ResponseEntity<IssuedCouponResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(IssuedCouponResponse.from(issuedCouponService.get(id)));
    }

    @GetMapping
    ResponseEntity<List<IssuedCouponResponse>> getAllByUsersId(@RequestParam Long usersId) {
        List<IssuedCouponResponse> responses = issuedCouponService.getAllByUsersId(usersId).stream()
                .map(IssuedCouponResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/use")
    ResponseEntity<Void> use(@PathVariable Long id) {
        issuedCouponService.use(id);
        return ResponseEntity.ok().build();
    }
}
