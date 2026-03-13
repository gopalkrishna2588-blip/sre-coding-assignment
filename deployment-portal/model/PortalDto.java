package com.sre.portal.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class PortalDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterServiceRequest {

        @NotBlank(message = "service_name is required")
        private String serviceName;

        @NotBlank(message = "team_name is required")
        private String teamName;

        @NotBlank(message = "repo_url is required")
        private String repoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterServiceResponse {
        private String serviceName;
        private String teamName;
        private String repoUrl;
        private List<String> createdResources;
        private Instant registeredAt;
    }
}