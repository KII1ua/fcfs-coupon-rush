package com.coupon.rush.coupon.policy.adapter.in.web;

import com.coupon.rush.coupon.policy.application.CouponPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policies")
public class CouponPolicyController {
    private final CouponPolicyService couponPolicyService;

    @PostMapping
    ResponseEntity<CouponPolicyCreateResponse> create(@Valid @RequestBody CouponPolicyCreateRequest req) {
        Long id = couponPolicyService.create(req.name(), req.discountValue(), req.maxDiscountAmount(), req.validUntil());
        return ResponseEntity.created(URI.create("/api/policies/" + id))
                .body(new CouponPolicyCreateResponse(id));
    }

    @GetMapping("/{id}")
    ResponseEntity<CouponPolicyResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(CouponPolicyResponse.from(couponPolicyService.get(id)));
    }

    @GetMapping
    ResponseEntity<List<CouponPolicyResponse>> getAll() {
        List<CouponPolicyResponse> body = couponPolicyService.getAll().stream()
                .map(CouponPolicyResponse::from)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody CouponPolicyUpdateRequest req) {
        couponPolicyService.update(id, req.name(), req.discountValue(), req.maxDiscountAmount(), req.validUntil());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        couponPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
