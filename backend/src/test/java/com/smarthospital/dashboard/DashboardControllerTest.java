package com.smarthospital.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class DashboardControllerTest {

    @Test
    void patientDashboardUsesAuthenticatedJwtSubject() {
        DashboardService service = mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);
        Jwt jwt = jwt("44");
        DashboardService.PatientDashboardResponse expected = new DashboardService.PatientDashboardResponse(
                LocalDate.of(2030, 1, 16),
                java.util.List.of(),
                null);
        when(service.patientDashboard(44L)).thenReturn(expected);

        assertThat(controller.patientDashboard(jwt)).isSameAs(expected);
        verify(service).patientDashboard(44L);
    }

    @Test
    void doctorDashboardUsesAuthenticatedJwtSubjectAndAcceptsNoDoctorId() throws Exception {
        DashboardService service = mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);
        Jwt jwt = jwt("55");
        DashboardService.DoctorDashboardResponse expected = new DashboardService.DoctorDashboardResponse(
                LocalDate.of(2030, 1, 16),
                java.util.List.of(),
                java.util.List.of());
        when(service.doctorDashboard(55L)).thenReturn(expected);

        assertThat(controller.doctorDashboard(jwt)).isSameAs(expected);
        verify(service).doctorDashboard(55L);

        Method doctorDashboard = DashboardController.class.getDeclaredMethod(
                "doctorDashboard",
                Jwt.class);
        assertThat(doctorDashboard.getParameterCount()).isEqualTo(1);
    }

    @Test
    void adminDashboardPassesOptionalSelectedDate() {
        DashboardService service = mock(DashboardService.class);
        DashboardController controller = new DashboardController(service);
        DashboardService.AdminDashboardResponse expected = new DashboardService.AdminDashboardResponse(
                LocalDate.of(2030, 1, 16),
                new DashboardService.AdminTotals(1, 1, 1, 2, 3));
        when(service.adminDashboard(null)).thenReturn(expected);
        when(service.adminDashboard(LocalDate.of(2030, 1, 20))).thenReturn(expected);

        assertThat(controller.adminDashboard(jwt("66"), null)).isSameAs(expected);
        assertThat(controller.adminDashboard(jwt("66"), LocalDate.of(2030, 1, 20)))
                .isSameAs(expected);
        verify(service).adminDashboard(null);
        verify(service).adminDashboard(LocalDate.of(2030, 1, 20));
    }

    private static Jwt jwt(String subject) {
        return Jwt.withTokenValue("access-token")
                .header("alg", "none")
                .claim("sub", subject)
                .build();
    }
}
