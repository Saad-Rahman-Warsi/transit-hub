package com.transithub.vehiclesservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehiclePosition {
    private String vehicleId;
    private String routeId;
    private String tripId;
    private String label;
    private double latitude;
    private double longitude;
    private Float bearing;       // degrees, 0=north, clockwise
    private Float speed;         // m/s
    private long timestamp;      // epoch seconds of last GPS ping
    private String occupancyStatus;
    private String currentStopId;
    private String currentStatus; // IN_TRANSIT_TO, STOPPED_AT, INCOMING_AT
}
