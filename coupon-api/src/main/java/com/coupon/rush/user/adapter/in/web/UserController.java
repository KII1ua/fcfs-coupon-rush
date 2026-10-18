package com.coupon.rush.user.adapter.in.web;

import com.coupon.rush.user.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping
    ResponseEntity<UserCreateResponse> register(@Valid @RequestBody UserCreateRequest req) {
        Long id = userService.register(req.userId(), req.password());
        return ResponseEntity.created(URI.create("/api/users/" + id))
                .body(new UserCreateResponse(id));
    }

    @GetMapping("/{id}")
    ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.get(id)));
    }
}
