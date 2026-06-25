package com.transithub.vehiclesservice.service;
 
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.transithub.vehiclesservice.model.VehiclePosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
 
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
 
/**
 * Fetches GTFS-RT VehiclePositions from the OC Transpo Azure API.
 *
 * Endpoint:
 *   https://nextrip-public-api.azure-api.net/octranspo/gtfs-rt-vp/beta/v1/VehiclePositions
 *
 * Proto field reference (gtfs-realtime.proto):
 *
 *   FeedEntity.vehicle → GtfsRealtime.VehiclePosition  (the proto message)
 *     .trip            → TripDescriptor  (trip_id, route_id, ...)
 *     .vehicle         → VehicleDescriptor (id, label, license_plate)
 *     .position        → Position (latitude, longitude, bearing, speed)
 *     .current_stop_sequence
 *     .stop_id         → hasStopId() / getStopId()
 *     .current_status  → VehicleStopStatus enum
 *     .timestamp       → hasTimestamp() / getTimestamp()
 *     .occupancy_status → OccupancyStatus enum  ← on VehiclePosition, NOT VehicleDescriptor
 *     .occupancy_percentage
 *
 *   CRITICAL: Do NOT call vp.getVehicle().getOccupancyStatus().
 *             VehicleDescriptor has no occupancy_status field.
 *             Call vp.hasOccupancyStatus() / vp.getOccupancyStatus() directly
 *             on the GtfsRealtime.VehiclePosition object.
 *
 * Note on naming: we use the fully-qualified proto class
 *   GtfsRealtime.VehiclePosition
 * to avoid collision with our own model class
 *   com.transithub.vehiclesservice.model.VehiclePosition
 */
@Service
@Slf4j
public class GtfsRtVehiclesService {
 
    private static final String VP_URL =
            "https://nextrip-public-api.azure-api.net/octranspo/gtfs-rt-vp/beta/v1/VehiclePositions?format=protobuf";
 
    @Value("08f0de09a1f4409bb7a2bd06798b935b")
    private String subscriptionKey;
 
    @Cacheable(value = "vehicles", key = "#routeId == null ? 'ALL' : #routeId")
    public List<VehiclePosition> getVehicles(String routeId) {
        log.info("Fetching GTFS-RT VehiclePositions (route={})", routeId);
        try {
            FeedMessage feed = fetchFeed();
 
            return feed.getEntityList().stream()
                    .filter(e -> e.hasVehicle())
                    .map(e -> {
                        // The proto VehiclePosition message — use fully-qualified name
                        // to avoid shadowing our own model class.
                        GtfsRealtime.VehiclePosition vp = e.getVehicle();
 
                        // ── Position ───────────────────────────────────────
                        double lat = 0, lon = 0;
                        Float bearing = null, speed = null;
                        if (vp.hasPosition()) {
                            lat     = vp.getPosition().getLatitude();
                            lon     = vp.getPosition().getLongitude();
                            bearing = vp.getPosition().hasBearing() ? vp.getPosition().getBearing() : null;
                            speed   = vp.getPosition().hasSpeed()   ? vp.getPosition().getSpeed()   : null;
                        }
 
                        // ── VehicleDescriptor ──────────────────────────────
                        // Fields: id, label, license_plate — NO occupancy here.
                        String vehicleId = e.getId(); // entity id as fallback
                        String label = null;
                        if (vp.hasVehicle()) {
                            if (vp.getVehicle().hasId())    vehicleId = vp.getVehicle().getId();
                            if (vp.getVehicle().hasLabel()) label     = vp.getVehicle().getLabel();
                        }
 
                        // ── TripDescriptor ─────────────────────────────────
                        String routeIdVal = null, tripIdVal = null;
                        if (vp.hasTrip()) {
                            routeIdVal = vp.getTrip().getRouteId();
                            tripIdVal  = vp.getTrip().getTripId();
                        }
 
                        // ── Occupancy ──────────────────────────────────────
                        // occupancy_status is a field on VehiclePosition (the proto message),
                        // not on VehicleDescriptor. Use vp.hasOccupancyStatus() directly.
                        String occupancy = "UNKNOWN";
                        if (vp.hasOccupancyStatus()) {
                            occupancy = vp.getOccupancyStatus().name();
                        }
 
                        // ── Stop status ────────────────────────────────────
                        // VehicleStopStatus enum: INCOMING_AT, STOPPED_AT, IN_TRANSIT_TO
                        String currentStatus = null;
                        if (vp.hasCurrentStatus()) {
                            currentStatus = vp.getCurrentStatus().name();
                        }
 
                        String currentStopId = vp.hasStopId() ? vp.getStopId() : null;
 
                        return VehiclePosition.builder()
                                .vehicleId(vehicleId)
                                .label(label)
                                .routeId(routeIdVal)
                                .tripId(tripIdVal)
                                .latitude(lat)
                                .longitude(lon)
                                .bearing(bearing)
                                .speed(speed)
                                .timestamp(vp.hasTimestamp() ? vp.getTimestamp() : 0L)
                                .occupancyStatus(occupancy)
                                .currentStopId(currentStopId)
                                .currentStatus(currentStatus)
                                .build();
                    })
                    .filter(v -> routeId == null || routeId.equals(v.getRouteId()))
                    .collect(Collectors.toList());
 
        } catch (Exception e) {
            log.error("Error fetching GTFS-RT VehiclePositions", e);
            return Collections.emptyList();
        }
    }
 
    @Scheduled(fixedRateString = "${gtfs.rt.cache-ttl-seconds:30}000")
    @CacheEvict(value = "vehicles", allEntries = true)
    public void evictCache() {
        log.debug("Evicted vehicles cache");
    }
 
    private FeedMessage fetchFeed() throws Exception {
        URL url = new URL(VP_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        try (InputStream is = conn.getInputStream()) {
            return FeedMessage.parseFrom(is);
        }
    }
}