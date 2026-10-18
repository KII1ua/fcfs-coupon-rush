package com.coupon.rush.coupon.round.adapter.in.web;

import com.coupon.rush.coupon.round.application.IssueRoundService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rounds")
public class IssueRoundController {
    private final IssueRoundService issueRoundService;

    @PostMapping
    ResponseEntity<IssueRoundCreateResponse> create(@Valid @RequestBody IssueRoundCreateRequest req) {
        Long id = issueRoundService.create(req.policyId(), req.openedAt(), req.closedAt(), req.totalQuantity());
        return ResponseEntity.created(URI.create("/api/rounds/" + id)).body(new IssueRoundCreateResponse(id));
    }

    @GetMapping("/{id}")
    ResponseEntity<IssueRoundResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(IssueRoundResponse.from(issueRoundService.get(id)));
    }

    @GetMapping
    ResponseEntity<List<IssueRoundResponse>> getAllByPolicyId(@RequestParam Long policyId) {
        List<IssueRoundResponse> rounds = issueRoundService.getAllByPolicyId(policyId).stream()
                .map(IssueRoundResponse::from)
                .toList();
        return ResponseEntity.ok(rounds);
    }

    @PutMapping("/{id}")
    ResponseEntity<Void> update(@PathVariable Long id, @Valid @RequestBody IssueRoundUpdateRequest req) {
        issueRoundService.update(id, req.openedAt(), req.closedAt(), req.totalQuantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        issueRoundService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
