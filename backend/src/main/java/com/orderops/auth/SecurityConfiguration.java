package com.orderops.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(AccountMapper accounts) {
        return username -> {
            Account account = accounts.findByUsername(username);
            if (account == null) throw new UsernameNotFoundException("Account not found");
            return User.withUsername(account.getUsername()).password(account.getPasswordHash())
                    .roles(account.getRole()).disabled(!account.isEnabled()).build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                .requestMatchers("/actuator/**", "/api/auth/csrf", "/api/auth/login").permitAll()
                .requestMatchers("/api/buyer/**").hasRole("BUYER")
                .requestMatchers("/api/operator/**").hasRole("OPERATOR")
                .anyRequest().authenticated())
            .csrf(Customizer.withDefaults())
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> writeError(response, 401, "UNAUTHENTICATED"))
                .accessDeniedHandler((request, response, error) -> writeError(response, 403, "FORBIDDEN")))
            .formLogin(login -> login.loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"authenticated\":true}");
                })
                .failureHandler((request, response, error) -> writeError(response, 401, "INVALID_CREDENTIALS")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("SESSION", "JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .requestCache(cache -> cache.disable());
        return http.build();
    }

    private static void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String code)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
