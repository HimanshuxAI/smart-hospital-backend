package com.smarthospital.department;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/departments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentController {

    private final DepartmentService departmentService;

    public AdminDepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return departmentService.create(request);
    }

    @PutMapping("/{departmentId}")
    public DepartmentResponse update(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(departmentId, request);
    }

    @PatchMapping("/{departmentId}/activation")
    public DepartmentResponse setActivation(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentActivationRequest request) {
        return departmentService.setActivation(departmentId, request.active());
    }

    @DeleteMapping("/{departmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long departmentId) {
        departmentService.deactivate(departmentId);
    }
}
