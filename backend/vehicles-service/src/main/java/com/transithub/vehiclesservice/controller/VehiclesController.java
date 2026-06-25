package com.transithub.vehiclesservice.controller;

import com.transithub.vehiclesservice.model.VehiclePosition;
import com.transithub.vehiclesservice.service.GtfsRtVehiclesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehiclesController {

    private final GtfsRtVehiclesService vehiclesService;

    /**
     * GET /api/vehicles?route=7
     * Returns live positions of all buses, optionally filtered by route number.
     */
    @GetMapping
    public List<VehiclePosition> getVehicles(
            @RequestParam(required = false) String route) {
        return vehiclesService.getVehicles(route);
    }

    /**
     * GET /api/vehicles/{vehicleId}
     * Returns live position of a specific vehicle by its ID.
     */
    @GetMapping("/{vehicleId}")
    public VehiclePosition getVehicle(@PathVariable String vehicleId) {
        return vehiclesService.getVehicles(null).stream()
                .filter(v -> vehicleId.equals(v.getVehicleId()))
                .findFirst()
                .orElse(null);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "vehicles-service");
    }
}
