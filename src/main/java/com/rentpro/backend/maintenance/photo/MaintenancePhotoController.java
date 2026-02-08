package com.rentpro.backend.maintenance.photo;

import com.rentpro.backend.maintenance.photo.dto.MaintenancePhotoResponse;
import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/maintenance")
public class MaintenancePhotoController {

    private final MaintenancePhotoService service;
    private final UserRepository userRepo;

    public MaintenancePhotoController(MaintenancePhotoService service, UserRepository userRepo) {
        this.service = service;
        this.userRepo = userRepo;
    }

    // TENANT uploads photo
    @PostMapping("/{id}/photos")
    @PreAuthorize("hasRole('TENANT')")
    public ResponseEntity<MaintenancePhotoResponse> upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) throws Exception {

        User tenant = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                MaintenancePhotoResponse.from(service.upload(tenant.getId(), id, file))
        );
    }

    // OWNER / TENANT view photos (return DTOs, not entity)
    @GetMapping("/{id}/photos")
    @PreAuthorize("hasAnyRole('TENANT','OWNER')")
    public ResponseEntity<List<MaintenancePhotoResponse>> list(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(
                service.list(id).stream().map(MaintenancePhotoResponse::from).toList()
        );
    }

    // serve image (use real content type if you want, for now keep jpeg)
    @GetMapping("/photos/{fileName}")
    public ResponseEntity<byte[]> view(@PathVariable String fileName) throws Exception {
        Path path = Path.of("uploads/maintenance").resolve(fileName);

        MediaType type = MediaType.APPLICATION_OCTET_STREAM;
        String probe = Files.probeContentType(path);
        if (probe != null) type = MediaType.parseMediaType(probe);

        return ResponseEntity.ok()
                .contentType(type)
                .body(Files.readAllBytes(path));
    }
}
