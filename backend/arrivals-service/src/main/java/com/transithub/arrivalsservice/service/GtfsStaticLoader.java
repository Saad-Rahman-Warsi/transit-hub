package com.transithub.arrivalsservice.service;
 
import com.opencsv.CSVReader;
import com.transithub.arrivalsservice.model.GtfsStaticRouteInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
 
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
 
/**
 * Parses the OC Transpo GTFS static zip to build lookup tables used by
 * GtfsRtArrivalsService to enrich raw GTFS-RT data with human-readable names.
 *
 * ── Stop code vs stop_id ─────────────────────────────────────────────────────
 * Per OC Transpo's GTFS developer release notes, stop_id in the new GTFS static
 * is a 4-character NUMERIC value — identical to the rider-facing stop_code
 * (e.g. "3009"). The GTFS-RT StopTimeUpdate.stop_id field uses that same value.
 *
 * Therefore throughout this entire microservice we use only ONE identifier:
 *   stopCode  (e.g. "3009")
 *
 * There is no translation between stop_code and stop_id. They are the same.
 * The word "stop_id" only appears in comments that reference the GTFS spec field
 * name; all Java variables, method parameters, and map keys use "stopCode".
 *
 * ── What this loader builds ──────────────────────────────────────────────────
 * tripInfoByTripId  — trip_id → GtfsStaticRouteInfo
 *                     Lets the arrivals service look up route_short_name and
 *                     trip_headsign for every TripUpdate in the GTFS-RT feed.
 *
 * (stops.txt is parsed by the stop-service, not here. This service only needs
 *  route and trip data.)
 */
@Service
@Slf4j
public class GtfsStaticLoader {
 
    @Value("${gtfs.static.url:https://www.octranspo.com/files/google_transit.zip}")
    private String gtfsZipUrl;
 
    /** trip_id → enriched route + headsign info */
    private volatile Map<String, GtfsStaticRouteInfo> tripInfoByTripId = Collections.emptyMap();
 
    @PostConstruct
    public void init() {
        load();
    }
 
    /** Reload every 24 hours — GTFS static changes at most weekly */
    @Scheduled(fixedRateString = "PT24H")
    public void scheduledReload() {
        load();
    }
 
    // ── Public API ────────────────────────────────────────────────────────────
 
    /**
     * Look up route and headsign info for a trip_id from GTFS-RT.
     * Returns empty if the trip is not in the current static schedule
     * (e.g. added trips, special service).
     */
    public Optional<GtfsStaticRouteInfo> getRouteInfoForTrip(String tripId) {
        return Optional.ofNullable(tripInfoByTripId.get(tripId));
    }
 
    // ── Loader ────────────────────────────────────────────────────────────────
 
    private void load() {
        log.info("Loading GTFS static routes + trips from {}", gtfsZipUrl);
        try {
            // Two separate passes through the zip (ZipInputStream is forward-only).
            // Pass 1: routes.txt  →  routeId → [shortName, longName]
            // Pass 2: trips.txt   →  tripId  → GtfsStaticRouteInfo (joined with routes)
            Map<String, String[]> routes = parseRoutes();
            Map<String, GtfsStaticRouteInfo> trips = parseTrips(routes);
 
            this.tripInfoByTripId = Collections.unmodifiableMap(trips);
            log.info("Loaded {} routes, {} trips from GTFS static", routes.size(), trips.size());
 
        } catch (Exception e) {
            log.error("Failed to load GTFS static — keeping previous data if any", e);
        }
    }
 
    /**
     * Parse routes.txt.
     * Returns map: routeId → String[2] { routeShortName, routeLongName }
     */
    private Map<String, String[]> parseRoutes() throws Exception {
        Map<String, String[]> map = new HashMap<>();
        try (ZipInputStream zis = openZip()) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("routes.txt".equals(entry.getName())) {
                    try (CSVReader csv = new CSVReader(new InputStreamReader(zis))) {
                        Map<String, Integer> idx = index(csv.readNext());
                        String[] row;
                        while ((row = csv.readNext()) != null) {
                            String routeId        = col(row, idx, "route_id");
                            String routeShortName = col(row, idx, "route_short_name");
                            String routeLongName  = col(row, idx, "route_long_name");
                            if (!routeId.isBlank()) {
                                map.put(routeId, new String[]{ routeShortName, routeLongName });
                            }
                        }
                    }
                    break;
                }
                zis.closeEntry();
            }
        }
        return map;
    }
 
    /**
     * Parse trips.txt, join with routes map.
     * Returns map: tripId → GtfsStaticRouteInfo
     *
     * Headsign priority:
     *   1. trips.txt  trip_headsign      (trip-specific destination, per OC Transpo docs)
     *   2. routes.txt route_long_name    (general corridor name, fallback)
     *   3. route_id                      (last resort)
     */
    private Map<String, GtfsStaticRouteInfo> parseTrips(Map<String, String[]> routes) throws Exception {
        Map<String, GtfsStaticRouteInfo> map = new HashMap<>();
        try (ZipInputStream zis = openZip()) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("trips.txt".equals(entry.getName())) {
                    try (CSVReader csv = new CSVReader(new InputStreamReader(zis))) {
                        Map<String, Integer> idx = index(csv.readNext());
                        String[] row;
                        while ((row = csv.readNext()) != null) {
                            String tripId     = col(row, idx, "trip_id");
                            String routeId    = col(row, idx, "route_id");
                            String headsign   = col(row, idx, "trip_headsign");
                            int    dirId      = parseInt(col(row, idx, "direction_id"));
 
                            if (tripId.isBlank()) continue;
 
                            String[] route        = routes.getOrDefault(routeId, new String[]{ routeId, "" });
                            String routeShortName = route[0].isBlank() ? routeId : route[0];
                            String routeLongName  = route[1];
 
                            String effectiveHeadsign =
                                    !headsign.isBlank()       ? headsign
                                  : !routeLongName.isBlank()  ? routeLongName
                                  : routeId;
 
                            map.put(tripId, GtfsStaticRouteInfo.builder()
                                    .tripId(tripId)
                                    .routeId(routeId)
                                    .routeShortName(routeShortName)
                                    .routeLongName(routeLongName)
                                    .tripHeadsign(effectiveHeadsign)
                                    .directionId(dirId)
                                    .build());
                        }
                    }
                    break;
                }
                zis.closeEntry();
            }
        }
        return map;
    }
 
    // ── Helpers ───────────────────────────────────────────────────────────────
 
    private ZipInputStream openZip() throws Exception {
        return new ZipInputStream(new URL(gtfsZipUrl).openStream());
    }
 
    private Map<String, Integer> index(String[] headers) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < headers.length; i++) m.put(headers[i].trim(), i);
        return m;
    }
 
    private String col(String[] row, Map<String, Integer> idx, String field) {
        Integer i = idx.get(field);
        return (i != null && i < row.length) ? row[i].trim() : "";
    }
 
    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}