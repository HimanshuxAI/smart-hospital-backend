package com.smarthospital.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.user.AdminUserResponse;
import com.smarthospital.user.CreateDoctorRequest;
import com.smarthospital.user.DoctorProfileResponse;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SensitiveValueRedactionTests {

    @Test
    void passwordBearingRecordsRedactPasswords() {
        String password = "SensitivePassword@123";

        RegisterRequest registerRequest = new RegisterRequest(
                "Patient Name",
                "patient@example.test",
                password,
                "+1-555-0100",
                LocalDate.of(1990, 1, 15),
                "Unspecified",
                "100 Test Street",
                "Emergency Contact +1-555-0199");
        LoginRequest loginRequest = new LoginRequest("patient@example.test", password);
        CreateDoctorRequest createDoctorRequest = new CreateDoctorRequest(
                "doctor@example.test",
                password,
                "Doctor Name",
                "+1-555-0101",
                1L,
                "Cardiology",
                "LICENSE-001",
                30);

        assertThat(registerRequest.toString()).doesNotContain(password).contains("REDACTED");
        assertThat(loginRequest.toString()).doesNotContain(password).contains("REDACTED");
        assertThat(createDoctorRequest.toString()).doesNotContain(password).contains("REDACTED");
    }

    @Test
    void piiBearingRecordsOmitPersonalAndLicensingDetails() {
        String email = "private.person@example.test";
        String fullName = "Private Person";
        String phone = "+1-555-0198";
        String address = "999 Private Street";
        String emergencyContact = "Private Emergency Contact";
        String licenseNumber = "PRIVATE-LICENSE-001";

        RegisterRequest registerRequest = new RegisterRequest(
                fullName,
                email,
                "StrongPassword@123",
                phone,
                LocalDate.of(1990, 1, 15),
                "Unspecified",
                address,
                emergencyContact);
        CreateDoctorRequest createDoctorRequest = new CreateDoctorRequest(
                email,
                "StrongPassword@123",
                fullName,
                phone,
                1L,
                "Cardiology",
                licenseNumber,
                30);
        UserSummaryResponse userSummary = new UserSummaryResponse(1L, email, UserRole.PATIENT);
        AdminUserResponse adminUser = new AdminUserResponse(
                2L,
                email,
                fullName,
                UserRole.ADMIN,
                true,
                phone);
        DoctorProfileResponse doctorProfile = new DoctorProfileResponse(
                3L,
                2L,
                email,
                fullName,
                phone,
                UserRole.DOCTOR,
                true,
                1L,
                "Cardiology",
                "Cardiology",
                licenseNumber,
                30,
                true);

        assertThat(registerRequest.toString())
                .doesNotContain(
                        fullName,
                        email,
                        phone,
                        "1990-01-15",
                        address,
                        emergencyContact)
                .contains("REDACTED");
        assertThat(createDoctorRequest.toString())
                .doesNotContain(fullName, email, phone, licenseNumber)
                .contains("REDACTED");
        assertThat(userSummary.toString()).doesNotContain(email).contains("REDACTED");
        assertThat(adminUser.toString())
                .doesNotContain(fullName, email, phone)
                .contains("REDACTED");
        assertThat(doctorProfile.toString())
                .doesNotContain(fullName, email, phone, licenseNumber)
                .contains("REDACTED");
    }

    @Test
    void tokenBearingRecordsRedactRawTokensAndHashes() {
        String rawToken = "A".repeat(43);
        String tokenHash = "b".repeat(64);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(rawToken);
        TokenBundleResponse tokenBundle = new TokenBundleResponse(
                "access-token",
                rawToken,
                "Bearer",
                900,
                new UserSummaryResponse(1L, "patient@example.test", UserRole.PATIENT));
        RefreshTokenService.IssuedRefreshToken issuedToken = new RefreshTokenService.IssuedRefreshToken(
                rawToken,
                tokenHash,
                "family-id",
                Instant.EPOCH);
        RefreshTokenService.RotationResult rotationResult = new RefreshTokenService.RotationResult(
                null,
                rawToken,
                tokenHash,
                "family-id",
                Instant.EPOCH);

        assertThat(refreshRequest.toString()).doesNotContain(rawToken).contains("REDACTED");
        assertThat(tokenBundle.toString())
                .doesNotContain("access-token", rawToken)
                .contains("REDACTED");
        assertThat(issuedToken.toString()).doesNotContain(rawToken, tokenHash).contains("REDACTED");
        assertThat(rotationResult.toString()).doesNotContain(rawToken, tokenHash).contains("REDACTED");
    }

    @Test
    void currentUserRecordOmitsProfileDetailsFromStringRepresentation() {
        CurrentUserResponse.PatientProfileSummary patientProfile =
                new CurrentUserResponse.PatientProfileSummary(
                        2L,
                        LocalDate.of(1990, 1, 15),
                        "Unspecified",
                        "100 Private Street",
                        "Emergency Contact +1-555-0199");
        CurrentUserResponse.DoctorProfileSummary doctorProfile =
                new CurrentUserResponse.DoctorProfileSummary(
                        3L,
                        4L,
                        "Cardiology",
                        "Cardiology",
                        "LICENSE-001",
                        30,
                        true);
        CurrentUserResponse currentUser = new CurrentUserResponse(
                1L,
                "patient@example.test",
                "Patient Name",
                UserRole.PATIENT,
                true,
                patientProfile,
                doctorProfile);

        assertThat(currentUser.toString())
                .doesNotContain(
                        "patient@example.test",
                        "Patient Name",
                        "100 Private Street",
                        "Emergency Contact +1-555-0199",
                        "LICENSE-001")
                .contains("REDACTED");
        assertThat(patientProfile.toString())
                .doesNotContain("100 Private Street", "Emergency Contact +1-555-0199")
                .contains("REDACTED");
        assertThat(doctorProfile.toString()).doesNotContain("LICENSE-001").contains("REDACTED");
    }
}
