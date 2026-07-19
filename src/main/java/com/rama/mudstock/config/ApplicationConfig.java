package com.rama.mudstock.config;

import java.time.ZoneId;

public final class ApplicationConfig {

    public static final String LISBON_ZONE = "Europe/Lisbon";
    public static final ZoneId LISBON = ZoneId.of(LISBON_ZONE);
    public static final String TIME_FORMAT_HH_MM = "HH:mm";

    private ApplicationConfig() {
        // Utility class
    }
}
