package com.aidigital.marketplace.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.aidigital.marketplace.identity.security.JwtAuthFilter;
import com.aidigital.marketplace.shared.web.JsonErrorWriter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JsonErrorWriter jsonErrorWriter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, JsonErrorWriter jsonErrorWriter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jsonErrorWriter = jsonErrorWriter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::unauthorized)
                        .accessDeniedHandler(this::forbidden))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ping", "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .requestMatchers("/api/v1/payments/notify/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories", "/api/v1/announcements")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/uploads/**")
                        .permitAll()
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException ex)
            throws IOException {
        jsonErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "未登录或令牌无效");
    }

    private void forbidden(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException ex)
            throws IOException {
        jsonErrorWriter.write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "没有权限");
    }
}
