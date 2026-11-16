package com.smarthospital.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "smart-hospital.clinical.check-in")
public class ClinicalCheckInProperties {

    @Min(0)
    private int earlyMinutes = 30;

    @Min(0)
    private int lateMinutes = 30;

    public int getEarlyMinutes() {
        return earlyMinutes;
    }

    public void setEarlyMinutes(int earlyMinutes) {
        this.earlyMinutes = earlyMinutes;
    }

    public int getLateMinutes() {
        return lateMinutes;
    }

    public void setLateMinutes(int lateMinutes) {
        this.lateMinutes = lateMinutes;
    }
}
