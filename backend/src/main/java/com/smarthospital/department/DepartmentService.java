package com.smarthospital.department;

import com.smarthospital.common.ApiException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw nameAlreadyExists();
        }
        try {
            Department department = departmentRepository.saveAndFlush(new Department(
                    name,
                    normalizeDescription(request.description())));
            return toResponse(department);
        } catch (DataIntegrityViolationException exception) {
            throw nameAlreadyExists();
        }
    }

    @Transactional
    public DepartmentResponse update(Long departmentId, DepartmentRequest request) {
        Department department = findDepartment(departmentId);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, departmentId)) {
            throw nameAlreadyExists();
        }
        department.setName(name);
        department.setDescription(normalizeDescription(request.description()));
        try {
            return toResponse(departmentRepository.saveAndFlush(department));
        } catch (DataIntegrityViolationException exception) {
            throw nameAlreadyExists();
        }
    }

    @Transactional
    public DepartmentResponse setActivation(Long departmentId, boolean active) {
        Department department = findDepartment(departmentId);
        department.setActive(active);
        return toResponse(departmentRepository.saveAndFlush(department));
    }

    @Transactional
    public void deactivate(Long departmentId) {
        setActivation(departmentId, false);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listActive() {
        return departmentRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    private Department findDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DEPARTMENT_NOT_FOUND",
                        "Department was not found"));
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.isActive(),
                department.getCreatedAt(),
                department.getUpdatedAt());
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static ApiException nameAlreadyExists() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "DEPARTMENT_NAME_ALREADY_EXISTS",
                "Department name is already in use");
    }
}
