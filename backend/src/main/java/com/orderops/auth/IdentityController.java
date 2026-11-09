package com.orderops.auth;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class IdentityController {
    private final AccountMapper accounts;
    public IdentityController(AccountMapper accounts) { this.accounts = accounts; }

    @GetMapping("/api/auth/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping({"/api/auth/me", "/api/buyer/me", "/api/operator/me"})
    Identity me(Principal principal) {
        Account account = accounts.findByUsername(principal.getName());
        if (account == null || !account.isEnabled()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return new Identity(account.getId(), account.getUsername(), account.getRole());
    }
    public record Identity(long id, String username, String role) { }
}
