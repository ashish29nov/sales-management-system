package com.sales.management.config;

import com.sales.management.service.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // ==============================
    // ADMIN SECURITY
    // ==============================

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurity(HttpSecurity http)
            throws Exception {

        http
            .securityMatcher("/admin/**")

            // DEVELOPMENT: Disable CSRF
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/admin/login")
                .permitAll()

                .requestMatchers("/admin/**")
                .hasRole("ADMIN")
            )

            .formLogin(form -> form

                .loginPage("/admin/login")

                .loginProcessingUrl("/admin/login")

                .usernameParameter("username")

                .passwordParameter("password")

                .defaultSuccessUrl("/admin/dashboard", true)

                .failureUrl("/admin/login?error=true")

                .permitAll()
            )

            .logout(logout -> logout

                .logoutUrl("/admin/logout")

                .logoutSuccessUrl("/admin/login?logout=true")

                .permitAll()
            )

            .userDetailsService(userDetailsService);

        return http.build();
    }


    // ==============================
    // SALESPERSON SECURITY
    // ==============================

    @Bean
    @Order(2)
    public SecurityFilterChain salespersonSecurity(HttpSecurity http)
            throws Exception {

        http
            .securityMatcher("/sales/**")

            // DEVELOPMENT: Disable CSRF
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/sales/login")
                .permitAll()

                .requestMatchers("/sales/**")
                .hasRole("SALESPERSON")
            )

            .formLogin(form -> form

                .loginPage("/sales/login")

                .loginProcessingUrl("/sales/login")

                .usernameParameter("username")

                .passwordParameter("password")

                .defaultSuccessUrl("/sales/dashboard", true)

                .failureUrl("/sales/login?error=true")

                .permitAll()
            )

            .logout(logout -> logout

                .logoutUrl("/sales/logout")

                .logoutSuccessUrl("/sales/login?logout=true")

                .permitAll()
            )

            .userDetailsService(userDetailsService);

        return http.build();
    }


    // ==============================
    // GENERAL
    // ==============================

    @Bean
    @Order(3)
    public SecurityFilterChain generalSecurity(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/",
                    "/css/**",
                    "/js/**",
                    "/images/**"
                ).permitAll()

                .anyRequest()
                .permitAll()
            );

        return http.build();
    }
}