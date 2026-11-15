package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.smarthospital.common.ApiException;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class AuthServiceTest extends MySqlIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoSpyBean
    private PatientProfileRepository patientProfileRepository;

    @Test
    void patientRegistrationCreatesUserProfileHashAndTokenBundle() {
        String email = uniqueEmail();
        RegisterRequest request = registration(email, "StrongPassword@123");

        TokenBundleResponse bundle = authService.register(request);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        PatientProfile profile = patientProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(user.getRole()).isEqualTo(UserRole.PATIENT);
        assertThat(user.getEmail()).isEqualTo(email.toLowerCase());
        assertThat(user.getPasswordHash()).isNotEqualTo(request.password());
        assertThat(user.getPasswordHash()).startsWith("$2");
        assertThat(passwordEncoder.matches(request.password(), user.getPasswordHash())).isTrue();
        assertThat(profile.getUser().getId()).isEqualTo(user.getId());
        assertThat(profile.getDateOfBirth()).isEqualTo(request.dateOfBirth());
        assertThat(bundle.accessToken()).startsWith("eyJ");
        assertThat(bundle.refreshToken()).isNotBlank();
        assertThat(bundle.tokenType()).isEqualTo("Bearer");
        assertThat(bundle.expiresIn()).isEqualTo(900);
        assertThat(bundle.user().id()).isEqualTo(user.getId());
        assertThat(bundle.user().email()).isEqualTo(email.toLowerCase());
        assertThat(bundle.user().role()).isEqualTo(UserRole.PATIENT);
        assertThat(refreshTokenRepository.findAll())
                .anySatisfy(token -> {
                    assertThat(token.getTokenHash()).hasSize(64);
                    assertThat(token.getTokenHash()).isNotEqualTo(bundle.refreshToken());
                    assertThat(token.getUser().getId()).isEqualTo(user.getId());
                });
    }

    @Test
    void duplicateEmailRegistrationReturnsStableConflictWithoutPartialData() {
        String email = uniqueEmail();
        authService.register(registration(email, "StrongPassword@123"));
        long usersBefore = userRepository.count();
        long profilesBefore = patientProfileRepository.count();

        assertThatThrownBy(() -> authService.register(registration(email.toUpperCase(), "OtherPassword@123")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(409);
                    assertThat(exception.code()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(patientProfileRepository.count()).isEqualTo(profilesBefore);
    }

    @Test
    void profilePersistenceFailureRollsBackTheUserCreation() {
        String email = uniqueEmail();
        doThrow(new IllegalStateException("synthetic profile persistence failure"))
                .when(patientProfileRepository)
                .saveAndFlush(any(PatientProfile.class));

        assertThatThrownBy(() -> authService.register(registration(email, "StrongPassword@123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic profile persistence failure");

        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();
    }

    @Test
    void passwordLongerThanSeventyTwoUtf8BytesIsRejectedWithoutTruncation() {
        String password = "é".repeat(37);

        assertThatThrownBy(() -> authService.register(registration(uniqueEmail(), password)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(400);
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.fieldErrors()).containsKey("password");
                });
    }

    @Test
    void loginPasswordLongerThanSeventyTwoUtf8BytesIsRejectedAsValidationFailure() {
        String password = "é".repeat(37);

        assertThatThrownBy(() -> authService.login(new LoginRequest(uniqueEmail(), password)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(400);
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.fieldErrors()).containsKey("password");
                });
    }

    @Test
    void invalidLoginRequestIsRejectedBeforeCredentialLookup() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("not-an-email", "")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(400);
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.fieldErrors()).containsKeys("email", "password");
                });
    }

    @Test
    void validLoginReturnsTokenBundle() {
        String email = uniqueEmail();
        String password = "StrongPassword@123";
        authService.register(registration(email, password));

        TokenBundleResponse bundle = authService.login(new LoginRequest(email, password));

        assertThat(bundle.accessToken()).startsWith("eyJ");
        assertThat(bundle.refreshToken()).isNotBlank();
        assertThat(bundle.tokenType()).isEqualTo("Bearer");
        assertThat(bundle.expiresIn()).isEqualTo(900);
        assertThat(bundle.user().email()).isEqualTo(email.toLowerCase());
        assertThat(bundle.user().role()).isEqualTo(UserRole.PATIENT);
    }

    @Test
    void unknownEmailAndWrongPasswordHaveTheSameStableFailure() {
        String email = uniqueEmail();
        String password = "StrongPassword@123";
        authService.register(registration(email, password));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "WrongPassword@123")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(401);
                    assertThat(exception.code()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(exception.getMessage()).isEqualTo("Invalid email or password");
                });
        assertThatThrownBy(() -> authService.login(new LoginRequest(uniqueEmail(), password)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(401);
                    assertThat(exception.code()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(exception.getMessage()).isEqualTo("Invalid email or password");
                });
    }

    @Test
    void refreshRotatesTheRefreshTokenAndReturnsANewBundle() {
        String email = uniqueEmail();
        TokenBundleResponse original = authService.register(registration(email, "StrongPassword@123"));

        TokenBundleResponse refreshed = authService.refresh(
                new RefreshTokenRequest(original.refreshToken()));

        assertThat(refreshed.accessToken()).startsWith("eyJ");
        assertThat(refreshed.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(refreshed.tokenType()).isEqualTo("Bearer");
        assertThat(refreshed.expiresIn()).isEqualTo(900);
        assertThat(refreshed.user().email()).isEqualTo(email.toLowerCase());
    }

    @Test
    void inactiveAccountCannotRefresh() {
        String email = uniqueEmail();
        TokenBundleResponse original = authService.register(registration(email, "StrongPassword@123"));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(original.refreshToken())))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("ACCOUNT_INACTIVE");
                });
    }

    @Test
    void logoutRevokesTheFamilyAndPreventsRefresh() {
        String email = uniqueEmail();
        TokenBundleResponse original = authService.register(registration(email, "StrongPassword@123"));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        authService.logout(user.getId(), original.refreshToken());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(original.refreshToken())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void currentUserReturnsIdentityAndPatientProfileSummaryWithoutPassword() {
        String email = uniqueEmail();
        TokenBundleResponse bundle = authService.register(registration(email, "StrongPassword@123"));

        CurrentUserResponse currentUser = authService.currentUser(bundle.user().id());

        assertThat(currentUser.id()).isEqualTo(bundle.user().id());
        assertThat(currentUser.email()).isEqualTo(email.toLowerCase());
        assertThat(currentUser.fullName()).isEqualTo("Test Patient");
        assertThat(currentUser.role()).isEqualTo(UserRole.PATIENT);
        assertThat(currentUser.active()).isTrue();
        assertThat(currentUser.patientProfile()).isNotNull();
        assertThat(currentUser.patientProfile().dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 15));
        assertThat(currentUser.patientProfile().address()).isEqualTo("100 Test Street");
        assertThat(currentUser.doctorProfile()).isNull();
        assertThat(currentUser.toString()).doesNotContain("StrongPassword@123");
    }

    @Test
    void inactiveAccountCannotLogIn() {
        String email = uniqueEmail();
        String password = "StrongPassword@123";
        authService.register(registration(email, password));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, password)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("ACCOUNT_INACTIVE");
                });
    }

    private static RegisterRequest registration(String email, String password) {
        return new RegisterRequest(
                "Test Patient",
                email,
                password,
                "+1-555-0100",
                LocalDate.of(1990, 1, 15),
                "Unspecified",
                "100 Test Street",
                "Emergency Contact +1-555-0199");
    }

    private static String uniqueEmail() {
        return "patient-" + UUID.randomUUID() + "@example.test";
    }
}
