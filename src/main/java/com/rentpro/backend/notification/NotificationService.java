package com.rentpro.backend.notification;

import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    public NotificationService(NotificationRepository repo, UserRepository userRepo, EmailService emailService) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    @Transactional
    public void create(Long userId, String type, String title, String message,
                       String entityType, Long entityId, boolean sendEmail) {

        User user = userRepo.findById(userId).orElseThrow();

        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .isRead(false)
                .build();

        repo.save(n);

        if (sendEmail) {
            emailService.safeSend(user.getEmail(), title, message);
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> listLatest(Long userId) {
        return repo.findTop50ByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> listUnread(Long userId) {
        return repo.findTop50ByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void setRead(Long userId, Long notificationId, boolean read) {
        Notification n = repo.findById(notificationId).orElseThrow();
        if (!n.getUser().getId().equals(userId)) throw new RuntimeException("Not yours");
        n.setRead(read);
        repo.save(n);
    }
}
