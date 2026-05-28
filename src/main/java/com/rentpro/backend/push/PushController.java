package com.rentpro.backend.push;

import com.rentpro.backend.security.JwtUserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping("/token")
    public ResponseEntity<?> registerToken(@RequestBody Map<String, String> body, Authentication auth) {
        UUID userId = getAuthUserId(auth);
        String token = body.get("token");
        String platform = body.getOrDefault("platform", "ANDROID");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token is required"));
        }
        pushService.registerToken(userId, token, platform);
        return ResponseEntity.ok(Map.of("status", "registered"));
    }

    @DeleteMapping("/token")
    public ResponseEntity<?> removeToken(@RequestBody(required = false) Map<String, String> body, Authentication auth) {
        UUID userId = getAuthUserId(auth);
        if (body != null && body.containsKey("token")) {
            pushService.removeToken(userId, body.get("token"));
        } else {
            pushService.removeAllTokens(userId);
        }
        return ResponseEntity.noContent().build();
    }

    private UUID getAuthUserId(Authentication auth) {
        JwtUserContext ctx = (JwtUserContext) auth.getDetails();
        return UUID.fromString(ctx.userId());
    }
}
