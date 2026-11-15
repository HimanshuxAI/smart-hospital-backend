package com.smarthospital.auth;

import com.smarthospital.common.ApiException;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final Validator validator;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PatientProfileRepository patientProfileRepository,
            DoctorProfileRepository doctorProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            Validator validator) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.validator = validator;
        this.dummyPasswordHash = passwordEncoder.encode("not-a-real-user-password");
    }

    @Transactional
    public TokenBundleResponse register(RegisterRequest request) {
        validate(request);
        validateBcryptPasswordLength(request.password());

        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw emailAlreadyExists();
        }

        User user;
        try {
            user = userRepository.saveAndFlush(new User(
                    UserRole.PATIENT,
                    request.fullName().trim(),
                    normalizedEmail,
                    passwordEncoder.encode(request.password()),
                    request.phone().trim()));
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyExists();
        }

        PatientProfile profile = new PatientProfile(user, request.dateOfBirth());
        profile.setGender(trimToNull(request.gender()));
        profile.setAddress(trimToNull(request.address()));
        profile.setEmergencyContact(trimToNull(request.emergencyContact()));
        patientProfileRepository.saveAndFlush(profile);

        return issueTokenBundle(user);
    }

    public TokenBundleResponse login(LoginRequest request) {
        validate(request);
        validateBcryptPasswordLength(request.password());
        String normalizedEmail = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        boolean passwordMatches = user == null
                ? passwordEncoder.matches(request.password(), dummyPasswordHash)
                : passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (user == null || !passwordMatches) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    INVALID_CREDENTIALS_MESSAGE);
        }
        if (!user.isActive()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_INACTIVE",
                    "Account is inactive");
        }
        return issueTokenBundle(user);
    }

    public TokenBundleResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(request.refreshToken());
        return tokenBundle(rotation.user(), rotation.rawToken());
    }

    public void logout(Long userId, String rawRefreshToken) {
        refreshTokenService.revokeFamily(rawRefreshToken, userId);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Authentication is required"));
        if (!user.isActive()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_INACTIVE",
                    "Account is inactive");
        }

        CurrentUserResponse.PatientProfileSummary patientProfile = null;
        CurrentUserResponse.DoctorProfileSummary doctorProfile = null;
        if (user.getRole() == UserRole.PATIENT) {
            PatientProfile profile = patientProfileRepository.findByUserId(userId).orElse(null);
            if (profile != null) {
                patientProfile = new CurrentUserResponse.PatientProfileSummary(
                        profile.getId(),
                        profile.getDateOfBirth(),
                        profile.getGender(),
                        profile.getAddress(),
                        profile.getEmergencyContact());
            }
        } else if (user.getRole() == UserRole.DOCTOR) {
            DoctorProfile profile = doctorProfileRepository.findByUserId(userId).orElse(null);
            if (profile != null) {
                doctorProfile = new CurrentUserResponse.DoctorProfileSummary(
                        profile.getId(),
                        profile.getDepartment().getId(),
                        profile.getDepartment().getName(),
                        profile.getSpecialty(),
                        profile.getLicenseNumber(),
                        profile.getAverageConsultationMinutes(),
                        profile.isActive());
            }
        }
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                patientProfile,
                doctorProfile);
    }

    private TokenBundleResponse issueTokenBundle(User user) {
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return tokenBundle(user, refreshToken.rawToken());
    }

    private TokenBundleResponse tokenBundle(User user, String rawRefreshToken) {
        String accessToken = jwtService.issueAccessToken(user);
        return new TokenBundleResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                jwtService.accessTokenExpiresInSeconds(),
                new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole()));
    }

    private void validate(Object request) {
        Set<ConstraintViolation<Object>> violations = validator.validate(request);
        if (violations.isEmpty()) {
            return;
        }
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<Object> violation : violations) {
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

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
