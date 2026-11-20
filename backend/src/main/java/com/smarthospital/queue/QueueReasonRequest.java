package com.smarthospital.queue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueueReasonRequest(@NotBlank @Size(max = 500) String reason) {
}
