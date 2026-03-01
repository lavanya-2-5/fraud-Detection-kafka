package com.lavanya.fraudDetection.dashboard.controller;

import com.lavanya.fraudDetection.dashboard.service.MetricsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fraud")
@CrossOrigin
public class FraudMetricsController {

    private final MetricsService metricsService;

    public FraudMetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        return metricsService.getMetrics();
    }
}