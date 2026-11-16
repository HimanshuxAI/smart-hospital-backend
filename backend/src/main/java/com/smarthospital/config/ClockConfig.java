package com.smarthospital.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ZoneId hospitalZoneId(@Value("${smart-hospital.time-zone:UTC}") String configuredZoneId) {
        return ZoneId.of(configuredZoneId);
    }
}
