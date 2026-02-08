package com.rentpro.backend.maintenance.photo;

import com.rentpro.backend.maintenance.MaintenanceRequest;
import com.rentpro.backend.maintenance.MaintenanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MaintenancePhotoService {

    private static final String UPLOAD_DIR = "uploads/maintenance";

    private final MaintenanceRepository maintenanceRepo;
    private final MaintenancePhotoRepository photoRepo;

    public MaintenancePhotoService(
            MaintenanceRepository maintenanceRepo,
            MaintenancePhotoRepository photoRepo
    ) {
        this.maintenanceRepo = maintenanceRepo;
        this.photoRepo = photoRepo;
    }

    @Transactional
    public MaintenancePhoto upload(Long tenantId, Long requestId, MultipartFile file) throws Exception {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is required");
        }

        MaintenanceRequest req = maintenanceRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        // tenant can upload only their own request
        if (!req.getTenant().getId().equals(tenantId)) {
            throw new RuntimeException("Not your maintenance request");
        }

        Files.createDirectories(Paths.get(UPLOAD_DIR));

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
        }

        String fileName = UUID.randomUUID() + ext;

        Path target = Paths.get(UPLOAD_DIR).resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        MaintenancePhoto photo = MaintenancePhoto.builder()
                .request(req)
                .fileName(fileName)
                .contentType(file.getContentType())
                .uploadedAt(Instant.now())
                .build();

        return photoRepo.save(photo);
    }

    @Transactional(readOnly = true)
    public List<MaintenancePhoto> list(Long requestId) {
        return photoRepo.findAllByRequest_Id(requestId);
    }
}
