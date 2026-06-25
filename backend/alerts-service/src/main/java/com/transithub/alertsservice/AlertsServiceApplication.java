package com.transithub.alertsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class AlertsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlertsServiceApplication.class, args);
    }
}
