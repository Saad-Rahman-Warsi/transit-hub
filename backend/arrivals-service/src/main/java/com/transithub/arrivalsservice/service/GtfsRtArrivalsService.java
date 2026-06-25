package com.transithub.arrivalsservice.service;
 
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.transithub.arrivalsservice.model.Arrival;
import com.transithub.arrivalsservice.model.GtfsStaticRouteInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
 
/**
 * Fetches GTFS-RT TripUpdates and returns upcoming arrivals for a stop.
 *
 * ── The only stop identifier used here: stopCode ─────────────────────────────
 * The caller (frontend → API gateway → this service) always passes a stopCode —
 * the rider-facing 4-digit number printed on the bus stop sign (e.g. "3009").
 *
 * In OC Transpo's GTFS static (confirmed in their developer release notes):
 *   stop_id = 4-character numeric value  ≡  stop_code  (they are identical)
 *
 * In the GTFS-RT TripUpdates feed:
 *   StopTimeUpdate.stop_id references that same 4-character numeric value.
 *
 * Therefore:
 *   - We match StopTimeUpdate.stop_id directly against the stopCode parameter.
 *   - No translation map, no resolveStopId(), no dual identifiers anywhere.
 *   - Every Java variable, parameter, and map key in this class uses "stopCode".
 *
 * ── Route name enrichment ─────────────────────────────────────────────────────
 * Raw GTFS-RT TripUpdates only carry trip_id and route_id — no human-readable
 * names. GtfsStaticLoader pre-parses routes.txt and trips.txt so we can join
 * on trip_id and attach:
 *   routeShortName  (e.g. "7")
 *   tripHeadsign    (e.g. "South Keys via Hurdman")
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GtfsRtArrivalsService {
 
    private static final String TRIP_UPDATES_URL =
            "https://nextrip-public-api.azure-api.net/octranspo/gtfs-rt-tp/beta/v1/TripUpdates";
 
    @Value("08f0de09a1f4409bb7a2bd06798b935b")
    private String subscriptionKey;
 
    private final GtfsStaticLoader staticLoader;
 
    private static final ZoneId     OTTAWA_TZ = ZoneId.of("America/Toronto");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("h:mm a").withZone(OTTAWA_TZ);
 

    public static String getStopCodeFromStopId(String stopId, String stopsFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(stopsFilePath))) {

            String line;
            br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",", -1);

                if (cols.length >= 2 && stopId.equals(cols[0].trim())) {
                    return cols[1].trim();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ── Public API ────────────────────────────────────────────────────────────
 
    /**
     * Returns upcoming arrivals at the given stopCode, sorted by predicted
     * arrival time ascending.
     *
     * @param stopCode  the rider-facing stop number, e.g. "3009"
     * @param limit     maximum number of arrivals to return
     */
    @Cacheable(value = "arrivals", key = "#stopCode + '-' + #limit")
    public List<Arrival> getArrivalsForStop(String stopCode, int limit) {
        log.info("Fetching arrivals for stopCode={}", stopCode);
        try {
            FeedMessage feed  = fetchFeed();
            long nowEpoch     = Instant.now().getEpochSecond();
            List<Arrival> results = new ArrayList<>();
 
            for (var entity : feed.getEntityList()) {
                if (!entity.hasTripUpdate()) continue;
                TripUpdate tu = entity.getTripUpdate();
 
                for (StopTimeUpdate stu : tu.getStopTimeUpdateList()) {
                    /*
                     * Match condition:
                     *   StopTimeUpdate.stop_id is the 4-digit numeric stop_id from
                     *   GTFS static stops.txt. For OC Transpo this equals stop_code.
                     *   We compare directly against stopCode — no mapping needed.
                     */
                    if (!stopCode.equals(getStopCodeFromStopId(stu.getStopId(),"stops.txt"))) continue;
 
                    // ── Arrival time ─────────────────────────────────────────
                    boolean hasArrivalTime   = stu.hasArrival()   && stu.getArrival().hasTime();
                    boolean hasDepartureTime = stu.hasDeparture() && stu.getDeparture().hasTime();
                    if (!hasArrivalTime && !hasDepartureTime) continue;
 
                    long scheduledEpoch = hasArrivalTime
                            ? stu.getArrival().getTime()
                            : stu.getDeparture().getTime();
 
                    if (scheduledEpoch < nowEpoch) continue; // already departed
 
                    // ── Delay ────────────────────────────────────────────────
                    int delaySeconds = 0;
                    if (hasArrivalTime && stu.getArrival().hasDelay()) {
                        delaySeconds = stu.getArrival().getDelay();
                    } else if (hasDepartureTime && stu.getDeparture().hasDelay()) {
                        delaySeconds = stu.getDeparture().getDelay();
                    }
 
                    long predictedEpoch   = scheduledEpoch + delaySeconds;
                    long minutesUntil     = (predictedEpoch - nowEpoch) / 60;
 
                    // ── Trip identity from GTFS-RT ───────────────────────────
                    String tripId  = tu.getTrip().getTripId();
                    String routeId = tu.getTrip().getRouteId();
 
                    // ── Route name + headsign from GTFS static ───────────────
                    // Join on trip_id to get human-readable route_short_name and
                    // trip_headsign. Falls back to raw routeId if trip not found
                    // (e.g. added/unscheduled trips not yet in static schedule).
                    GtfsStaticRouteInfo info = staticLoader
                            .getRouteInfoForTrip(tripId)
                            .orElse(null);
 
                    String routeShortName = (info != null) ? info.getRouteShortName() : routeId;
                    String tripHeadsign   = (info != null) ? info.getTripHeadsign()   : routeId;
 
                    // ── Vehicle id ───────────────────────────────────────────
                    // VehicleDescriptor fields: id, label, license_plate only.
                    // No occupancy here — occupancy lives in the VehiclePositions feed.
                    String vehicleId = null;
                    if (tu.hasVehicle() && tu.getVehicle().hasId()) {
                        vehicleId = tu.getVehicle().getId();
                    }
 
                    results.add(Arrival.builder()
                            .routeId(routeId)
                            .routeShortName(routeShortName)
                            .headsign(tripHeadsign)
                            .tripId(tripId)
                            .scheduledArrivalEpoch(scheduledEpoch)
                            .predictedArrivalEpoch(predictedEpoch)
                            .delaySeconds(delaySeconds)
                            .isRealTime(delaySeconds != 0)
                            .vehicleId(vehicleId)
                            // Occupancy is UNKNOWN here; the frontend can cross-reference
                            // with the vehicles-service using vehicleId if needed.
                            .occupancyStatus("UNKNOWN")
                            .minutesUntilArrival(minutesUntil)
                            .arrivalLabel(buildLabel(minutesUntil, predictedEpoch))
                            .build());
 
                    break; // one match per trip per stop is sufficient
                }
            }
 
            results.sort(Comparator.comparingLong(Arrival::getPredictedArrivalEpoch));
            return results.stream().limit(limit).collect(Collectors.toList());
 
        } catch (Exception e) {
            log.error("Error fetching GTFS-RT TripUpdates for stopCode={}", stopCode, e);
            return Collections.emptyList();
        }
    }
 
    @Scheduled(fixedRateString = "${gtfs.rt.cache-ttl-seconds:30}000")
    @CacheEvict(value = "arrivals", allEntries = true)
    public void evictArrivalsCache() {
        log.debug("Evicted arrivals cache");
    }
 
    // ── Helpers ───────────────────────────────────────────────────────────────
 
    private FeedMessage fetchFeed() throws Exception {
        URL url = new URL(TRIP_UPDATES_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        try (InputStream is = conn.getInputStream()) {
            return FeedMessage.parseFrom(is);
        }
    }
 
    private String buildLabel(long minutes, long epochSec) {
        if (minutes <= 0)  return "Now";
        if (minutes <= 30) return minutes + " min";
        return TIME_FMT.format(Instant.ofEpochSecond(epochSec));
    }
}