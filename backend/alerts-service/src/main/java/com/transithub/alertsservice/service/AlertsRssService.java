package com.transithub.alertsservice.service;

import com.transithub.alertsservice.model.ServiceAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

/**
 * Parses OC Transpo service alerts from their public RSS feed.
 * Feed URL: https://www.octranspo.com/en/alerts/rss
 * No API key required for RSS.
 */
@Service
@Slf4j
public class AlertsRssService {

    private static final String RSS_URL = "https://www.octranspo.com/en/alerts/rss";
    // Matches route numbers like "Route 7", "Routes 1, 2, 3", "#95"
    private static final Pattern ROUTE_PATTERN =
            Pattern.compile("(?:Route[s]?\\s*#?|#)(\\d+(?:,\\s*\\d+)*)", Pattern.CASE_INSENSITIVE);

    @Cacheable("alerts")
    public List<ServiceAlert> getAlerts() {
        log.info("Fetching OC Transpo service alerts RSS");
        try {
            var dbf = DocumentBuilderFactory.newInstance();
            var db = dbf.newDocumentBuilder();
            var doc = db.parse(new URL(RSS_URL).openStream());
            doc.getDocumentElement().normalize();

            List<ServiceAlert> alerts = new ArrayList<>();
            NodeList items = doc.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = text(item, "title");
                String description = text(item, "description");
                String pubDate = text(item, "pubDate");
                String link = text(item, "link");
                String guid = text(item, "guid");

                List<String> routes = extractRoutes(title + " " + description);
                String severity = inferSeverity(title, description);

                alerts.add(ServiceAlert.builder()
                        .id(guid.isBlank() ? String.valueOf(i) : guid)
                        .title(title)
                        .description(stripHtml(description))
                        .severity(severity)
                        .affectedRoutes(routes)
                        .publishedAt(pubDate)
                        .link(link)
                        .build());
            }
            log.info("Loaded {} service alerts", alerts.size());
            return Collections.unmodifiableList(alerts);

        } catch (Exception e) {
            log.error("Failed to fetch alerts RSS", e);
            return Collections.emptyList();
        }
    }

    /** Filter alerts affecting a specific route number */
    public List<ServiceAlert> getAlertsForRoute(String routeId) {
        return getAlerts().stream()
                .filter(a -> a.getAffectedRoutes().contains(routeId))
                .toList();
    }

    @Scheduled(fixedRateString = "PT5M")   // refresh every 5 minutes
    @CacheEvict(value = "alerts", allEntries = true)
    public void evictCache() {
        log.debug("Evicted alerts cache");
    }

    // ---- helpers ----

    private String text(Element parent, String tag) {
        var nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return "";
        return nodes.item(0).getTextContent().trim();
    }

    private List<String> extractRoutes(String text) {
        List<String> routes = new ArrayList<>();
        Matcher m = ROUTE_PATTERN.matcher(text);
        while (m.find()) {
            for (String part : m.group(1).split(",")) {
                routes.add(part.trim());
            }
        }
        return routes;
    }

    private String inferSeverity(String title, String description) {
        String combined = (title + " " + description).toLowerCase();
        if (combined.contains("cancelled") || combined.contains("suspended") || combined.contains("no service"))
            return "SEVERE";
        if (combined.contains("delay") || combined.contains("detour") || combined.contains("diversion"))
            return "WARNING";
        return "INFO";
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "").replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<").replaceAll("&gt;", ">")
                .replaceAll("&nbsp;", " ").trim();
    }
}
