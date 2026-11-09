package com.orderops.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class DemoAccountsConfigurationTest {
    @Test
    void optInConfigurationLoadsWithPasswords() {
        var mapper = mock(AccountMapper.class);
        new ApplicationContextRunner()
                .withUserConfiguration(DemoAccountsConfiguration.class)
                .withBean(AccountMapper.class, () -> mapper)
                .withBean(org.springframework.security.crypto.password.PasswordEncoder.class, BCryptPasswordEncoder::new)
                .withPropertyValues("orderops.demo-accounts.enabled=true",
                        "orderops.demo-accounts.buyer-password=buyer-secret",
                        "orderops.demo-accounts.operator-password=operator-secret")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(org.springframework.boot.ApplicationRunner.class));
    }
    @Test
    void acceptsDocumentedEnvironmentVariableNames() {
        new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                        new org.springframework.core.env.SystemEnvironmentPropertySource("demo-env", java.util.Map.of(
                                "ORDEROPS_DEMO_ACCOUNTS_ENABLED", "true",
                                "ORDEROPS_DEMO_ACCOUNTS_BUYER_PASSWORD", "buyer-secret",
                                "ORDEROPS_DEMO_ACCOUNTS_OPERATOR_PASSWORD", "operator-secret"))))
                .withUserConfiguration(DemoAccountsConfiguration.class)
                .withBean(AccountMapper.class, () -> mock(AccountMapper.class))
                .withBean(org.springframework.security.crypto.password.PasswordEncoder.class, BCryptPasswordEncoder::new)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(org.springframework.boot.ApplicationRunner.class));
    }
    @Test
    void createsRoleSpecificAccountsWithHashedPasswords() throws Exception {
        AccountMapper accounts = mock(AccountMapper.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        new DemoAccountsConfiguration().createDemoAccounts(accounts, encoder, "buyer-secret", "operator-secret").run(null);
        verify(accounts).insertIfAbsent(eq("buyer"), argThat(hash -> encoder.matches("buyer-secret", hash)), eq("BUYER"));
        verify(accounts).insertIfAbsent(eq("operator"), argThat(hash -> encoder.matches("operator-secret", hash)), eq("OPERATOR"));
        assertThat(encoder.matches("buyer-secret", encoder.encode("buyer-secret"))).isTrue();
    }
    @Test
    void rejectsEmptyOrReusedDemoPasswords() {
        var configuration = new DemoAccountsConfiguration();
        var accounts = mock(AccountMapper.class);
        var encoder = new BCryptPasswordEncoder();
        assertThatThrownBy(() -> configuration.createDemoAccounts(accounts, encoder, "", "other"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.createDemoAccounts(accounts, encoder, "same", "same"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(accounts);
    }
}
