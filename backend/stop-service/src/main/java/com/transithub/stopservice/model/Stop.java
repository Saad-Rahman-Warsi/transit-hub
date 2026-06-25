package com.transithub.stopservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stop {
    private String stopId;
    private String stopCode;
    private String stopName;
    private double lat;
    private double lon;
    private String zoneId;
    // Computed at runtime
    private Double distanceMetres;
    private int routeCount;
}
