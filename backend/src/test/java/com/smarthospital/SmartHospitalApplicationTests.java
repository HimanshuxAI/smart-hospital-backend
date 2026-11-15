package com.smarthospital;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smarthospital;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "smart-hospital.security.jwt.secret=test-jwt-secret-with-at-least-32-characters"
})
class SmartHospitalApplicationTests {

    @Test
    void contextLoads() {
    }
}
