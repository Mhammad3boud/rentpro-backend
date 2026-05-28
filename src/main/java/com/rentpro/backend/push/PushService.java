package com.rentpro.backend.push;

import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fcm.server.key:}")
    private String fcmServerKey;

    public PushService(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void registerToken(UUID userId, String token, String platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean exists = deviceTokenRepository.findByUser_UserId(userId)
                .stream().anyMatch(dt -> dt.getToken().equals(token));
        if (exists) return;

        DeviceToken dt = new DeviceToken();
        dt.setUser(user);
        dt.setToken(token);
        dt.setPlatform(platform != null ? platform : "ANDROID");
        deviceTokenRepository.save(dt);
    }

    @Transactional
    public void removeToken(UUID userId, String token) {
        deviceTokenRepository.deleteByUser_UserIdAndToken(userId, token);
    }

    @Transactional
    public void removeAllTokens(UUID userId) {
        deviceTokenRepository.deleteByUser_UserId(userId);
    }

    public void sendToUser(UUID userId, String title, String body, String route) {
        if (fcmServerKey == null || fcmServerKey.isBlank()) {
            System.err.println("[PUSH] FCM_SERVER_KEY not set — skipping push to user " + userId);
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUser_UserId(userId);
        if (tokens.isEmpty()) return;

        for (DeviceToken dt : tokens) {
            try {
                sendFcm(dt.getToken(), title, body, route);
            } catch (Exception e) {
                System.err.println("[PUSH] Failed to send to token " + dt.getToken() + ": " + e.getMessage());
            }
        }
    }

    private void sendFcm(String token, String title, String body, String route) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=" + fcmServerKey);

        Map<String, Object> notification = Map.of("title", title, "body", body);
        Map<String, Object> data = route != null ? Map.of("route", route) : Map.of();
        Map<String, Object> payload = Map.of("to", token, "notification", notification, "data", data);

        restTemplate.exchange(
            "https://fcm.googleapis.com/fcm/send",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class
        );
    }
}
