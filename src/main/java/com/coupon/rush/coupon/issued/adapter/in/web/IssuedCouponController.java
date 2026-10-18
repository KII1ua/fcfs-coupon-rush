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

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/issued-coupons")
public class IssuedCouponController {
    private final IssuedCouponService issuedCouponService;

    @PostMapping
    ResponseEntity<IssueCouponResponse> issue(@Valid @RequestBody IssueCouponRequest req) {
        Long id = issuedCouponService.issue(req.roundId(), req.usersId());
        return ResponseEntity.created(URI.create("/api/issued-coupons/" + id))
                .body(new IssueCouponResponse(id));
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
