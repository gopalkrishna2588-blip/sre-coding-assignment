package com.sre.userservice.controller;

import com.sre.userservice.metrics.UserServiceMetrics;
import com.sre.userservice.model.UserDto;
import com.sre.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserServiceMetrics metrics;

    /**
     * POST /user
     * Header: Idempotency-Key (optional)
     */
    @PostMapping
    public ResponseEntity<?> createUser(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody UserDto.CreateUserRequest request) {

        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        metrics.incrementTotal();

        // Log: Request ID
        log.info("[RequestId={}] POST /user started", requestId);

        try {
            UserDto.UserResponse response = userService.createUser(requestId, idempotencyKey, request);
            metrics.incrementSuccess();

            // Log: Latency
            long latency = System.currentTimeMillis() - start;
            log.info("[RequestId={}] POST /user success. latency={}ms", requestId, latency);
            metrics.recordLatency(start);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            metrics.incrementFailure();
            long latency = System.currentTimeMillis() - start;

            // Log: Error summary
            log.error("[RequestId={}] POST /user failed. latency={}ms error={}", requestId, latency, e.getMessage());
            metrics.recordLatency(start);

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(errorResponse(requestId, "CONFLICT", e.getMessage()));

        } catch (Exception e) {
            metrics.incrementFailure();
            long latency = System.currentTimeMillis() - start;

            // Log: Error summary
            log.error("[RequestId={}] POST /user failed. latency={}ms error={}", requestId, latency, e.getMessage());
            metrics.recordLatency(start);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse(requestId, "SERVICE_UNAVAILABLE", e.getMessage()));
        }
    }

    /**
     * GET /user/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable("id") String userId) {

        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        metrics.incrementTotal();

        // Log: Request ID
        log.info("[RequestId={}] GET /user/{} started", requestId, userId);

        try {
            UserDto.UserResponse response = userService.getUser(requestId, userId);
            metrics.incrementSuccess();

            // Log: Latency
            long latency = System.currentTimeMillis() - start;
            log.info("[RequestId={}] GET /user/{} success. latency={}ms", requestId, userId, latency);
            metrics.recordLatency(start);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            metrics.incrementFailure();
            long latency = System.currentTimeMillis() - start;

            // Log: Error summary
            log.error("[RequestId={}] GET /user/{} failed. latency={}ms error={}", requestId, userId, latency, e.getMessage());
            metrics.recordLatency(start);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse(requestId, "NOT_FOUND", e.getMessage()));

        } catch (Exception e) {
            metrics.incrementFailure();
            long latency = System.currentTimeMillis() - start;

            // Log: Error summary
            log.error("[RequestId={}] GET /user/{} failed. latency={}ms error={}", requestId, userId, latency, e.getMessage());
            metrics.recordLatency(start);

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorResponse(requestId, "SERVICE_UNAVAILABLE", e.getMessage()));
        }
    }

    private UserDto.ErrorResponse errorResponse(String requestId, String error, String message) {
        return UserDto.ErrorResponse.builder()
                .requestId(requestId)
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}