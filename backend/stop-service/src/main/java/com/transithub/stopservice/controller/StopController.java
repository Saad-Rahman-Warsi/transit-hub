package com.transithub.stopservice.controller;

import com.transithub.stopservice.model.Stop;
import com.transithub.stopservice.service.GtfsStopLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stops")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StopController {

    private final GtfsStopLoader loader;

    /**
     * GET /api/stops/search?q=rideau&lat=45.42&lon=-75.69&limit=10
     * Full-text search by stop name or code, optionally sorted by proximity.
     */
    @GetMapping("/search")
    public List<Stop> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(defaultValue = "20") int limit) {
        return loader.search(q, lat, lon, limit);
    }

    /**
     * GET /api/stops/nearby?lat=45.42&lon=-75.69&radius=500&limit=10
     * Returns stops within radius metres, sorted by distance.
     */
    @GetMapping("/nearby")
    public List<Stop> nearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "500") int radius,
            @RequestParam(defaultValue = "10") int limit) {
        return loader.nearby(lat, lon, radius, limit);
    }

    /**
     * GET /api/stops/{code}
     * Lookup a single stop by its rider-facing stop code (e.g. "3009").
     */
    @GetMapping("/{code}")
    public ResponseEntity<Stop> byCode(@PathVariable String code) {
        return loader.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "stop-service");
    }
}
