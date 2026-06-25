package com.transithub.arrivalsservice.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Arrival {
    private String routeId;
    private String routeShortName;
    private String routeLongName;
    private String headsign;
    private String tripId;
    private long scheduledArrivalEpoch;   // seconds since epoch
    private Long predictedArrivalEpoch;   // null if no real-time data
    private int delaySeconds;             // positive = late, negative = early
    private boolean isRealTime;
    private String vehicleId;
    private String occupancyStatus;       // EMPTY, MANY_SEATS, FEW_SEATS, STANDING, CRUSHED, FULL
    // Computed convenience fields for the UI
    private long minutesUntilArrival;
    private String arrivalLabel;          // "Now", "3 min", "12:47"
}
