package com.sre.userservice.service;

import com.sre.userservice.model.User;
import com.sre.userservice.model.UserDto;
import com.sre.userservice.repository.UserRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ── POST /user ─────────────────────────────────────────────────────────────
    @Transactional
    public UserDto.UserResponse createUser(String requestId,
                                           String idempotencyKey,
                                           UserDto.CreateUserRequest request) {

        // STEP 1: Idempotency — same key = return existing user, no duplicate
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<User> existing = userRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("[{}] Idempotent request. Returning existing user.", requestId);
                return toResponse(existing.get());
            }
        }

        // STEP 2: Check duplicate userId
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            log.warn("[{}] Duplicate userId={}", requestId, request.getUserId());
            throw new IllegalArgumentException("user_id already exists: " + request.getUserId());
        }

        // STEP 3: Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[{}] Duplicate email={}", requestId, request.getEmail());
            throw new IllegalArgumentException("email already registered: " + request.getEmail());
        }

        // STEP 4: DB write — wrapped in circuit breaker + retry
        return dbWrite(requestId, idempotencyKey, request);
    }

    @CircuitBreaker(name = "dbWriteCircuitBreaker", fallbackMethod = "dbWriteFallback")
    @Retry(name = "dbWriteRetry")
    public UserDto.UserResponse dbWrite(String requestId,
                                        String idempotencyKey,
                                        UserDto.CreateUserRequest request) {
        User user = User.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .idempotencyKey(idempotencyKey)
                .build();

        User saved = userRepository.save(user);
        log.info("[{}] User created successfully. userId={}", requestId, saved.getUserId());
        return toResponse(saved);
    }

    public UserDto.UserResponse dbWriteFallback(String requestId,
                                                String idempotencyKey,
                                                UserDto.CreateUserRequest request,
                                                Throwable t) {
        log.error("[{}] Circuit open / retries exhausted. error={}", requestId, t.getMessage());
        throw new RuntimeException("DB write unavailable. Please retry later.");
    }

    // ── GET /user/{id} ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserDto.UserResponse getUser(String requestId, String userId) {
        log.info("[{}] Fetching userId={}", requestId, userId);
        return userRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    // ── Helper ─────────────────────────────────────────────────────────────────
    private UserDto.UserResponse toResponse(User user) {
        return UserDto.UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}