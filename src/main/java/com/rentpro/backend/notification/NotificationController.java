package com.rentpro.backend.notification;

import com.rentpro.backend.notification.dto.MarkReadRequest;
import com.rentpro.backend.notification.dto.NotificationResponse;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final UserRepository userRepo;

    public NotificationController(NotificationService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(Authentication auth) {
        User u = userRepo.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(service.listLatest(u.getId()).stream().map(NotificationResponse::from).toList());
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> unread(Authentication auth) {
        User u = userRepo.findByEmail(auth.getName()).orElseThrow();
        return ResponseEntity.ok(service.listUnread(u.getId()).stream().map(NotificationResponse::from).toList());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, @Valid @RequestBody MarkReadRequest req, Authentication auth) {
        User u = userRepo.findByEmail(auth.getName()).orElseThrow();
        service.setRead(u.getId(), id, req.read());
        return ResponseEntity.ok().build();
    }
}
