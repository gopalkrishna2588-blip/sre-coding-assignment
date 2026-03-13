package com.sre.userservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {

        @NotBlank(message = "user_id is required")
        private String userId;

        @NotBlank(message = "name is required")
        private String name;

        @Email
        @NotBlank(message = "email is required")
        private String email;

        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private String userId;
        private String name;
        private String email;
        private String phone;
        private Instant createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String requestId;
        private String error;
        private String message;
        private Instant timestamp;
    }
}