package com.sre.portal.controller;

import com.sre.portal.model.PortalDto;
import com.sre.portal.service.DeploymentPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class DeploymentPortalController {

    private final DeploymentPortalService portalService;

    @PostMapping("/register")
    public ResponseEntity<PortalDto.RegisterServiceResponse> registerService(
            @Valid @RequestBody PortalDto.RegisterServiceRequest request) {

        log.info("Received register request for service={}", request.getServiceName());
        PortalDto.RegisterServiceResponse response = portalService.registerService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}