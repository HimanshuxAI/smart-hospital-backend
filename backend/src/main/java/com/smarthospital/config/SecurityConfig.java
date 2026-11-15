package com.smarthospital.config;

import com.smarthospital.auth.JwtService;
import com.smarthospital.common.SecurityProblemHandler;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Set<String> ALLOWED_ROLES = Set.of("PATIENT", "DOCTOR", "ADMIN");

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProblemHandler securityProblemHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .requestCache(requestCache -> requestCache.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh")
                        .permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/v1/doctors/profile",
                                "/api/v1/doctors/*/profile")
                        .hasRole("DOCTOR")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(securityProblemHandler)
                        .accessDeniedHandler(securityProblemHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(securityProblemHandler)
                        .accessDeniedHandler(securityProblemHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
            if (role == null || !ALLOWED_ROLES.contains(role)) {
                return List.of();
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
