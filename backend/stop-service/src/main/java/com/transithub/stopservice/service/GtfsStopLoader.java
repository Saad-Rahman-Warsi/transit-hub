package com.transithub.stopservice.service;

import com.opencsv.CSVReader;
import com.transithub.stopservice.model.Stop;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

/**
 * Loads OC Transpo GTFS static schedule (stops.txt + routes.txt) from the
 * publicly available zip. Refreshes daily — GTFS static changes infrequently.
 *
 * GTFS zip URL: https://www.octranspo.com/files/google_transit.zip
 * No API key required for the static feed.
 */
@Service
@Slf4j
public class GtfsStopLoader {

    @Value("${gtfs.static.url:https://oct-gtfs-emasagcnfmcgeham.z01.azurefd.net/public-access/GTFSExport.zip}")
    private String gtfsZipUrl;

    // stopId -> Stop
    private volatile Map<String, Stop> stopsById = new HashMap<>();
    // stopCode -> Stop  (stop codes are what riders see on signs, e.g. "3009")
    private volatile Map<String, Stop> stopsByCode = new HashMap<>();

    @PostConstruct
    public void init() {
        loadGtfsStops();
    }

    /** Reload every 24 hours — GTFS static files update at most weekly */
    @Scheduled(fixedRateString = "PT24H")
    public void scheduledReload() {
        loadGtfsStops();
    }

    private void loadGtfsStops() {
        log.info("Loading GTFS static stops from {}", gtfsZipUrl);
        try {
            Map<String, Stop> byId = new HashMap<>();
            Map<String, Stop> byCode = new HashMap<>();

            URL url = new URL(gtfsZipUrl);
            try (ZipInputStream zis = new ZipInputStream(url.openStream())) {
                var entry = zis.getNextEntry();
                while (entry != null) {
                    if ("stops.txt".equals(entry.getName())) {
                        try (CSVReader csv = new CSVReader(new InputStreamReader(zis))) {
                            String[] headers = csv.readNext(); // skip header
                            Map<String, Integer> idx = indexHeaders(headers);
                            String[] row;
                            while ((row = csv.readNext()) != null) {
                                Stop stop = Stop.builder()
                                        .stopId(get(row, idx, "stop_id"))
                                        .stopCode(get(row, idx, "stop_code"))
                                        .stopName(get(row, idx, "stop_name"))
                                        .lat(parseDouble(get(row, idx, "stop_lat")))
                                        .lon(parseDouble(get(row, idx, "stop_lon")))
                                        .zoneId(get(row, idx, "zone_id"))
                                        .build();
                                byId.put(stop.getStopId(), stop);
                                if (stop.getStopCode() != null && !stop.getStopCode().isBlank()) {
                                    byCode.put(stop.getStopCode(), stop);
                                }
                            }
                        }
                        break; // stops.txt found, done
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
            }

            stopsById = Collections.unmodifiableMap(byId);
            stopsByCode = Collections.unmodifiableMap(byCode);
            log.info("Loaded {} stops from GTFS static", byId.size());

        } catch (Exception e) {
            log.error("Failed to load GTFS stops — keeping previous data if available", e);
        }
    }

    public Optional<Stop> findByCode(String code) {
        return Optional.ofNullable(stopsByCode.get(code));
    }

    public Optional<Stop> findById(String id) {
        return Optional.ofNullable(stopsById.get(id));
    }

    /**
     * Full-text search on stop name and code. Returns up to {@code limit} results,
     * optionally sorted by distance from (lat, lon).
     */
    public List<Stop> search(String query, Double userLat, Double userLon, int limit) {
        String q = query == null ? "" : query.toLowerCase().trim();
        return stopsById.values().stream()
                .filter(s -> q.isEmpty()
                        || s.getStopName().toLowerCase().contains(q)
                        || s.getStopCode().toLowerCase().contains(q))
                .map(s -> enrichWithDistance(s, userLat, userLon))
                .sorted(distanceOrNameComparator(userLat, userLon))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Stops within {@code radiusMetres} of (lat, lon), sorted by distance */
    public List<Stop> nearby(double lat, double lon, int radiusMetres, int limit) {
        return stopsById.values().stream()
                .map(s -> enrichWithDistance(s, lat, lon))
                .filter(s -> s.getDistanceMetres() != null && s.getDistanceMetres() <= radiusMetres)
                .sorted(Comparator.comparingDouble(Stop::getDistanceMetres))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private Stop enrichWithDistance(Stop s, Double lat, Double lon) {
        if (lat == null || lon == null) return s;
        Stop copy = Stop.builder()
                .stopId(s.getStopId()).stopCode(s.getStopCode())
                .stopName(s.getStopName()).lat(s.getLat()).lon(s.getLon())
                .zoneId(s.getZoneId())
                .distanceMetres(haversineMetres(lat, lon, s.getLat(), s.getLon()))
                .build();
        return copy;
    }

    private Comparator<Stop> distanceOrNameComparator(Double lat, Double lon) {
        if (lat != null && lon != null) {
            return Comparator.comparingDouble(s ->
                    s.getDistanceMetres() == null ? Double.MAX_VALUE : s.getDistanceMetres());
        }
        return Comparator.comparing(Stop::getStopName);
    }

    private double haversineMetres(double lat1, double lon1, double lat2, double lon2) {
        double R = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Map<String, Integer> indexHeaders(String[] headers) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < headers.length; i++) m.put(headers[i].trim(), i);
        return m;
    }

    private String get(String[] row, Map<String, Integer> idx, String field) {
        Integer i = idx.get(field);
        return (i != null && i < row.length) ? row[i].trim() : "";
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}
