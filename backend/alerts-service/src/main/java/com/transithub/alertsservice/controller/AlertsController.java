package com.transithub.alertsservice.controller;

import com.transithub.alertsservice.model.ServiceAlert;
import com.transithub.alertsservice.service.AlertsRssService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertsController {

    private final AlertsRssService alertsService;

    /**
     * GET /api/alerts
     * Returns all current service alerts.
     */
    @GetMapping
    public List<ServiceAlert> getAlerts() {
        return alertsService.getAlerts();
    }

    /**
     * GET /api/alerts?route=7
     * Returns alerts affecting a specific route number.
     */
    @GetMapping(params = "route")
    public List<ServiceAlert> getAlertsForRoute(@RequestParam String route) {
        return alertsService.getAlertsForRoute(route);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "alerts-service");
    }
}
