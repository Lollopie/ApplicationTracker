package com.jobtracker.main;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@EnableWebSecurity
@Configuration
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http.csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository()).spa())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/register", "/login").permitAll()
                        .requestMatchers("/actuator", "/actuator/health").permitAll()
                        .requestMatchers("/applications/**").authenticated()
                        .requestMatchers("/error/**").permitAll()
                        .requestMatchers("/logout").authenticated()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(new UnauthorizedAuthenticationEntryPoint()))
                .sessionManagement((session) -> session
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::newSession))
                .logout((logout) -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        return new CookieCsrfTokenRepository();
    }
}
