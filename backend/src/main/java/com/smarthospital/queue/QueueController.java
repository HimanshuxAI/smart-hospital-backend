package com.smarthospital.queue;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @GetMapping
    public List<QueueEntryResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return queueService.list(Long.valueOf(jwt.getSubject()), doctorId, date);
    }

    @GetMapping("/{entryId}")
    public QueueEntryResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId) {
        return queueService.get(Long.valueOf(jwt.getSubject()), entryId);
    }

    @PostMapping("/{entryId}/classify")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse classify(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId,
            @Valid @RequestBody ClassifyQueueEntryRequest request) {
        return queueService.classify(Long.valueOf(jwt.getSubject()), entryId, request);
    }

    @PostMapping("/{entryId}/override")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse override(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId,
            @Valid @RequestBody ClassifyQueueEntryRequest request) {
        return queueService.override(Long.valueOf(jwt.getSubject()), entryId, request);
    }

    @PostMapping("/{entryId}/call")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse call(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId) {
        return queueService.call(Long.valueOf(jwt.getSubject()), entryId);
    }

    @PostMapping("/{entryId}/serve")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse serve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId) {
        return queueService.serve(Long.valueOf(jwt.getSubject()), entryId);
    }

    @PostMapping("/{entryId}/complete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse complete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId) {
        return queueService.complete(Long.valueOf(jwt.getSubject()), entryId);
    }

    @PostMapping("/{entryId}/skip")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse skip(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId,
            @Valid @RequestBody QueueReasonRequest request) {
        return queueService.skip(
                Long.valueOf(jwt.getSubject()),
                entryId,
                request.reason());
    }

    @PostMapping("/{entryId}/return-to-waiting")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse returnToWaiting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId) {
        return queueService.returnToWaiting(Long.valueOf(jwt.getSubject()), entryId);
    }

    @PostMapping("/{entryId}/cancel")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public QueueEntryResponse cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long entryId,
            @Valid @RequestBody QueueReasonRequest request) {
        return queueService.cancel(
                Long.valueOf(jwt.getSubject()),
                entryId,
                request.reason());
    }
}
