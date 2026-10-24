package com.jobtracker.main;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@EnableWebSecurity
@Configuration
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final String cookiePath;
    public SecurityConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                          @Value("${server.servlet.session.cookie.path}") String cookiePath) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.cookiePath = cookiePath;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UnauthorizedAuthenticationEntryPoint unauthorizedAuthenticationEntryPoint) {
        // spa() installs its own token repository, so ours is set afterwards: the filter and the
        // token rotation in UserController#login must share one repository with the same cookie path.
        return http.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/register", "/login").permitAll()
                        .requestMatchers("/actuator", "/actuator/health").permitAll()
                        .requestMatchers("/applications/**").authenticated()
                        .requestMatchers("/error/**").permitAll()
                        .requestMatchers("/logout").authenticated()
                        .requestMatchers("/me").authenticated()
                        .requestMatchers("/csrf").permitAll()
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(unauthorizedAuthenticationEntryPoint))
                .sessionManagement((session) -> session
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::newSession))
                .logout((logout) -> logout.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()))
                .build();
    }



    @Bean
    public AuthenticationManager authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath(cookiePath);
        return repository;
    }
}
