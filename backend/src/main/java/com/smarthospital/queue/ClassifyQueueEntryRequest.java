package com.smarthospital.queue;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClassifyQueueEntryRequest(
        @NotNull @JsonAlias({"tier", "emergencyTier", "priority"}) EmergencyTier triageLevel,
        @NotBlank @Size(max = 500) String reason) {
}
