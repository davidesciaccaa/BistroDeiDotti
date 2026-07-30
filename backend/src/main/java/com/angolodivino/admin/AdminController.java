package com.angolodivino.admin;

import com.angolodivino.menu.MenuSectionResponse;
import com.angolodivino.menu.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API. Everything but /login is gated by {@link AdminAuthInterceptor}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminProperties adminProperties;
    private final AdminTokenStore tokenStore;
    private final AdminLoginThrottle loginThrottle;
    private final MenuService menuService;

    public AdminController(AdminProperties adminProperties, AdminTokenStore tokenStore,
            AdminLoginThrottle loginThrottle, MenuService menuService) {
        this.adminProperties = adminProperties;
        this.tokenStore = tokenStore;
        this.loginThrottle = loginThrottle;
        this.menuService = menuService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest request, HttpServletRequest httpRequest) {
        if (!adminProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AdminApiError("admin_disabled", "Pannello admin non configurato: manca ADMIN_PASSWORD."));
        }

        String client = clientKey(httpRequest);
        if (loginThrottle.isBlocked(client)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new AdminApiError("too_many_attempts", "Troppi tentativi falliti. Riprova tra qualche minuto."));
        }

        if (!matchesPassword(request.password())) {
            loginThrottle.recordFailure(client);
            log.warn("Failed admin login attempt from {}", client);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AdminApiError("invalid_password", "Password errata."));
        }

        loginThrottle.reset(client);
        AdminSession session = tokenStore.issue();
        log.info("Admin session issued to {} (expires {})", client, session.expiresAt());
        return ResponseEntity.ok(session);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        tokenStore.revoke(AdminTokenStore.bearerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    /** Lets the panel check a stored token on load without showing the login form first. */
    @GetMapping("/session")
    public AdminSessionResponse session(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Instant expiresAt = tokenStore.validate(AdminTokenStore.bearerToken(authorization)).orElse(null);
        return new AdminSessionResponse(expiresAt);
    }

    /** The same payload the public site renders, so the panel mirrors it exactly. */
    @GetMapping("/menu/sections")
    public List<MenuSectionResponse> sections() {
        return menuService.findMenuSections();
    }

    @PatchMapping("/menu/prices")
    public List<MenuSectionResponse> updatePrices(@Valid @RequestBody UpdatePricesRequest request) {
        Map<String, String> prices = request.prices();
        log.info("Admin updating {} menu price(s)", prices.size());
        return menuService.updatePrices(prices);
    }

    @PostMapping("/menu/items")
    public List<MenuSectionResponse> createItem(@Valid @RequestBody MenuItemRequest request) { return menuService.createItem(request); }

    @PatchMapping("/menu/items/{id}")
    public List<MenuSectionResponse> updateItem(@PathVariable String id, @Valid @RequestBody MenuItemRequest request) { return menuService.updateItem(id, request); }

    @DeleteMapping("/menu/items/{id}")
    public List<MenuSectionResponse> deleteItem(@PathVariable String id) { return menuService.deleteItem(id); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AdminApiError> handleInvalidRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new AdminApiError("invalid_request", e.getMessage()));
    }

    private boolean matchesPassword(String candidate) {
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                adminProperties.getPassword().getBytes(StandardCharsets.UTF_8));
    }

    private static String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record AdminSessionResponse(Instant expiresAt) {
    }
}
