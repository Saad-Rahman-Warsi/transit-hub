package com.transithub.alertsservice.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ServiceAlert {
    private String id;
    private String title;
    private String description;
    private String severity;        // INFO, WARNING, SEVERE
    private List<String> affectedRoutes;
    private String publishedAt;
    private String link;
}
