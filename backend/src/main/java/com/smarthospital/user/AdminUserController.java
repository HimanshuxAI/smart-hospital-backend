package com.smarthospital.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/doctors")
    @Operation(tags = "Admin", summary = "Create a doctor profile")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorProfileResponse createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return userService.createDoctor(request);
    }

    @GetMapping("/users")
    @Operation(tags = "Admin", summary = "List users")
    @SecurityRequirement(name = "bearerAuth")
    public List<AdminUserResponse> listUsers() {
        return userService.listUsers();
    }

    @PatchMapping("/users/{id}/activation")
    @Operation(tags = "Admin", summary = "Activate or deactivate a user")
    @SecurityRequirement(name = "bearerAuth")
    public AdminUserResponse setActivation(
            @PathVariable Long id,
            @Valid @RequestBody ActivationRequest request) {
        return userService.setActivation(id, request.active());
    }

    @GetMapping("/doctors/{doctorId}/profile")
    @Operation(tags = "Admin", summary = "Get a doctor profile")
    @SecurityRequirement(name = "bearerAuth")
    public DoctorProfileResponse getDoctorProfile(@PathVariable Long doctorId) {
        return userService.getDoctorProfile(doctorId);
    }

    @PutMapping("/doctors/{doctorId}/profile")
    @Operation(tags = "Admin", summary = "Update a doctor profile")
    @SecurityRequirement(name = "bearerAuth")
    public DoctorProfileResponse updateDoctorProfile(
            @PathVariable Long doctorId,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return userService.updateDoctorProfile(doctorId, request);
    }
}
