package com.smarthospital.user;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
@PreAuthorize("hasRole('DOCTOR')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public DoctorProfileResponse getOwnProfile(@AuthenticationPrincipal Jwt jwt) {
        return userService.getOwnDoctorProfile(Long.valueOf(jwt.getSubject()));
    }

    @PutMapping("/profile")
    public DoctorProfileResponse updateOwnProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return userService.updateOwnDoctorProfile(
                Long.valueOf(jwt.getSubject()),
                request);
    }

    @GetMapping("/{doctorId}/profile")
    public DoctorProfileResponse getProfile(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal Jwt jwt) {
        return userService.getDoctorProfileForDoctor(
                doctorId,
                Long.valueOf(jwt.getSubject()));
    }

    @PutMapping("/{doctorId}/profile")
    public DoctorProfileResponse updateProfile(
            @PathVariable Long doctorId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return userService.updateDoctorProfileForDoctor(
                doctorId,
                Long.valueOf(jwt.getSubject()),
                request);
    }
}
