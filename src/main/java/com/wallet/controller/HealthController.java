package com.wallet.controller;

import com.wallet.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = new HealthResponse(
                "UP",
                "p2p-digital-wallet",
                Instant.now()
        );
        return ResponseEntity.ok(response);
    }
}
