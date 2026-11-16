package com.smarthospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartHospitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartHospitalApplication.class, args);
    }
}
