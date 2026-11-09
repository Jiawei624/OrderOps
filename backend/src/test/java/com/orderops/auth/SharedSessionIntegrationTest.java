package com.orderops.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderops.BackendApplication;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "ORDEROPS_RUN_INTEGRATION", matches = "true")
@ActiveProfiles("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SharedSessionIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    final String suffix = UUID.randomUUID().toString().replace("-", "");
    final String buyer = "test_buyer_" + suffix;
    final String operator = "test_operator_" + suffix;
    final String password = UUID.randomUUID().toString();
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build();

    @BeforeAll
    void insertTestAccounts() {
        jdbc.update("INSERT INTO accounts(username, password_hash, role) VALUES (?, ?, 'BUYER')", buyer, encoder.encode(password));
        jdbc.update("INSERT INTO accounts(username, password_hash, role) VALUES (?, ?, 'OPERATOR')", operator, encoder.encode(password));
    }
    @AfterAll
    void deleteTestAccounts() {
        jdbc.update("DELETE FROM accounts WHERE username IN (?, ?)", buyer, operator);
    }
    HttpResponse<String> get(int targetPort, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + targetPort + path))
                .timeout(Duration.ofSeconds(30)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    HttpResponse<String> post(int targetPort, String path, String body, String csrf) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + targetPort + path))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/x-www-form-urlencoded");
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String csrf(int targetPort) throws Exception {
        HttpResponse<String> response = get(targetPort, "/api/auth/csrf");
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json.readTree(response.body());
        assertThat(body.path("headerName").asText()).isEqualTo("X-CSRF-TOKEN");
        return body.path("token").asText();
    }
    String form(String username, String secret) {
        return "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(secret, StandardCharsets.UTF_8);
    }
    String sessionCookie() {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("SESSION"))
                .findFirst().orElseThrow().getValue();
    }

    @Test
    void rolesCsrfAndSessionSurviveAcrossApplicationInstances() throws Exception {
        assertThat(get(port, "/api/auth/me").statusCode()).isEqualTo(401);
        String initialCsrf = csrf(port);
        String initialSession = sessionCookie();
        assertThat(post(port, "/api/auth/login", form(buyer, password), null).statusCode()).isEqualTo(403);
        assertThat(post(port, "/api/auth/login", form(buyer, "wrong"), initialCsrf).statusCode()).isEqualTo(401);
        assertThat(post(port, "/api/auth/login", form(buyer, password), initialCsrf).statusCode()).isEqualTo(200);
        assertThat(sessionCookie()).isNotEqualTo(initialSession);
        assertThat(json.readTree(get(port, "/api/buyer/me").body()).path("username").asText()).isEqualTo(buyer);
        assertThat(get(port, "/api/operator/me").statusCode()).isEqualTo(403);

        try (ConfigurableApplicationContext second = new SpringApplicationBuilder(BackendApplication.class)
                .profiles("integration").run("--server.port=0", "--logging.level.root=WARN")) {
            int otherPort = ((ServletWebServerApplicationContext) second).getWebServer().getPort();
            HttpResponse<String> otherNode = get(otherPort, "/api/auth/me");
            assertThat(otherNode.statusCode()).isEqualTo(200);
            assertThat(json.readTree(otherNode.body()).path("username").asText()).isEqualTo(buyer);
            String token = csrf(otherPort);
            java.net.HttpCookie previousSession = (java.net.HttpCookie) cookies.getCookieStore().getCookies().stream()
                    .filter(c -> c.getName().equals("SESSION")).findFirst().orElseThrow().clone();
            assertThat(post(otherPort, "/api/auth/logout", "", null).statusCode()).isEqualTo(403);
            assertThat(post(otherPort, "/api/auth/logout", "", token).statusCode()).isEqualTo(204);
            cookies.getCookieStore().add(URI.create("http://127.0.0.1:" + port), previousSession);
            assertThat(get(port, "/api/auth/me").statusCode()).isEqualTo(401);
        }
        String token = csrf(port);
        assertThat(post(port, "/api/auth/login", form(operator, password), token).statusCode()).isEqualTo(200);
        assertThat(get(port, "/api/operator/me").statusCode()).isEqualTo(200);
        assertThat(get(port, "/api/buyer/me").statusCode()).isEqualTo(403);
        assertThat(post(port, "/api/auth/logout", "", csrf(port)).statusCode()).isEqualTo(204);
    }
}
