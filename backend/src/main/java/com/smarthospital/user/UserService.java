package com.smarthospital.user;

import com.smarthospital.common.ApiException;
import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public UserService(
            UserRepository userRepository,
            DoctorProfileRepository doctorProfileRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            Validator validator) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional
    public DoctorProfileResponse createDoctor(CreateDoctorRequest request) {
        validate(request);
        validateBcryptPasswordLength(request.password());

        String normalizedEmail = normalizeEmail(request.email());
        String licenseNumber = request.licenseNumber().trim();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw emailAlreadyExists();
        }
        if (doctorProfileRepository.findByLicenseNumber(licenseNumber).isPresent()) {
            throw licenseAlreadyExists();
        }
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DEPARTMENT_NOT_FOUND",
                        "Department was not found"));
        if (!department.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DEPARTMENT_INACTIVE",
                    "Department is inactive");
        }

        User user;
        try {
            user = userRepository.saveAndFlush(new User(
                    UserRole.DOCTOR,
                    request.fullName().trim(),
                    normalizedEmail,
                    passwordEncoder.encode(request.password()),
                    request.phone().trim()));
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyExists();
        }

        DoctorProfile profile;
        try {
            profile = doctorProfileRepository.saveAndFlush(new DoctorProfile(
                    user,
                    department,
                    request.specialty().trim(),
                    licenseNumber,
                    request.averageConsultationMinutes()));
        } catch (DataIntegrityViolationException exception) {
            throw licenseAlreadyExists();
        }
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse setActivation(Long userId, boolean active) {
        User user = findUser(userId);
        user.setActive(active);
        return toUserResponse(userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getDoctorProfile(Long doctorProfileId) {
        return toResponse(findDoctorProfile(doctorProfileId));
    }

    @Transactional
    public DoctorProfileResponse updateDoctorProfile(
            Long doctorProfileId,
            UpdateDoctorProfileRequest request) {
        validate(request);
        DoctorProfile profile = findDoctorProfile(doctorProfileId);
        Department department = findActiveDepartment(request.departmentId());
        profile.setDepartment(department);
        profile.setSpecialty(request.specialty().trim());
        profile.setAverageConsultationMinutes(request.averageConsultationMinutes());
        return toResponse(doctorProfileRepository.saveAndFlush(profile));
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getOwnDoctorProfile(Long userId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));
        return toResponse(profile);
    }

    @Transactional
    public DoctorProfileResponse updateOwnDoctorProfile(
            Long userId,
            UpdateDoctorProfileRequest request) {
        validate(request);
        DoctorProfile profile = doctorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));
        Department department = findActiveDepartment(request.departmentId());
        profile.setDepartment(department);
        profile.setSpecialty(request.specialty().trim());
        profile.setAverageConsultationMinutes(request.averageConsultationMinutes());
        return toResponse(doctorProfileRepository.saveAndFlush(profile));
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getDoctorProfileForDoctor(
            Long requestedDoctorProfileId,
            Long currentUserId) {
        DoctorProfile ownProfile = doctorProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));
        if (!ownProfile.getId().equals(requestedDoctorProfileId)) {
            throw forbiddenProfileAccess();
        }
        return toResponse(ownProfile);
    }

    @Transactional
    public DoctorProfileResponse updateDoctorProfileForDoctor(
            Long requestedDoctorProfileId,
            Long currentUserId,
            UpdateDoctorProfileRequest request) {
        validate(request);
        DoctorProfile profile = doctorProfileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));
        if (!profile.getId().equals(requestedDoctorProfileId)) {
            throw forbiddenProfileAccess();
        }
        Department department = findActiveDepartment(request.departmentId());
        profile.setDepartment(department);
        profile.setSpecialty(request.specialty().trim());
        profile.setAverageConsultationMinutes(request.averageConsultationMinutes());
        return toResponse(doctorProfileRepository.saveAndFlush(profile));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "USER_NOT_FOUND",
                        "User was not found"));
    }

    private DoctorProfile findDoctorProfile(Long doctorProfileId) {
        return doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_PROFILE_NOT_FOUND",
                        "Doctor profile was not found"));
    }

    private Department findActiveDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DEPARTMENT_NOT_FOUND",
                        "Department was not found"));
        if (!department.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DEPARTMENT_INACTIVE",
                    "Department is inactive");
        }
        return department;
    }

    private DoctorProfileResponse toResponse(DoctorProfile profile) {
        User user = profile.getUser();
        return new DoctorProfileResponse(
                profile.getId(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                profile.getDepartment().getId(),
                profile.getDepartment().getName(),
                profile.getSpecialty(),
                profile.getLicenseNumber(),
                profile.getAverageConsultationMinutes(),
                profile.isActive());
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getPhone());
    }

    private void validate(Object request) {
        var violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : violations) {
            fieldErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                fieldErrors);
    }

    private void validateBcryptPasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    Map.of("password", "Password must not exceed 72 UTF-8 bytes"));
        }
    }

    private static ApiException emailAlreadyExists() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_EXISTS",
                "Email is already registered");
    }

    private static ApiException licenseAlreadyExists() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "DOCTOR_LICENSE_ALREADY_EXISTS",
                "Doctor license number is already registered");
    }

    private static ApiException forbiddenProfileAccess() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access is denied");
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
