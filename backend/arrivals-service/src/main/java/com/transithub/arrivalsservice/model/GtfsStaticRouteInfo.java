package com.transithub.arrivalsservice.model;
 
import lombok.Builder;
import lombok.Data;
 
/**
 * Holds the route and headsign data for a single trip, parsed from
 * OC Transpo GTFS static routes.txt + trips.txt.
 *
 * Used to enrich GTFS-RT TripUpdate entities with human-readable names
 * before returning arrival results to the frontend.
 *
 * Field sources:
 *   routeShortName  — routes.txt  route_short_name  (e.g. "7")
 *   routeLongName   — routes.txt  route_long_name   (e.g. "South Keys <> Bayshore")
 *   tripHeadsign    — trips.txt   trip_headsign     (e.g. "South Keys via Hurdman")
 *   directionId     — trips.txt   direction_id      (0 or 1)
 */
@Data
@Builder
public class GtfsStaticRouteInfo {
    private String tripId;
    private String routeId;
    private String routeShortName;
    private String routeLongName;
    private String tripHeadsign;
    private int    directionId;
}