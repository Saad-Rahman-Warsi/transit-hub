package com.transithub.arrivalsservice.controller;

import com.transithub.arrivalsservice.model.Arrival;
import com.transithub.arrivalsservice.model.GtfsStaticRouteInfo;
import com.transithub.arrivalsservice.service.GtfsRtArrivalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/arrivals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArrivalsController {

    private final GtfsRtArrivalsService arrivalsService;

    /**
     * GET /api/arrivals/{stopCode}?limit=10
     * Returns upcoming arrivals at a stop sorted by predicted arrival time.
     */
    @GetMapping("/{stopCode}")
    public List<Arrival> getArrivals(
            @PathVariable String stopCode,
            @RequestParam(defaultValue = "10") int limit) {
        return arrivalsService.getArrivalsForStop(stopCode, limit);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "arrivals-service");
    }
}
