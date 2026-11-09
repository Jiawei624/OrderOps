package com.orderops.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(name = "orderops.demo-accounts.enabled", havingValue = "true")
public class DemoAccountsConfiguration {
    @Bean
    ApplicationRunner createDemoAccounts(AccountMapper accounts, PasswordEncoder encoder,
            @Value("${orderops.demo-accounts.buyer-password}") String buyerPassword,
            @Value("${orderops.demo-accounts.operator-password}") String operatorPassword) {
        if (buyerPassword.isBlank() || operatorPassword.isBlank() || buyerPassword.equals(operatorPassword)) {
            throw new IllegalStateException("Demo account passwords must be nonempty and distinct");
        }
        return args -> {
            accounts.insertIfAbsent("buyer", encoder.encode(buyerPassword), "BUYER");
            accounts.insertIfAbsent("operator", encoder.encode(operatorPassword), "OPERATOR");
        };
    }
}
