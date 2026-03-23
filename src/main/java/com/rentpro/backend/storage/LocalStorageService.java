package com.rentpro.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir:uploads/maintenance}")
    private String maintenanceUploadDir;

    @Value("${app.upload.profiles.dir:uploads/profiles}")
    private String profileUploadDir;

    @Override
    public String uploadProfilePicture(String filename, MultipartFile file) throws IOException {
        Path dir = Paths.get(profileUploadDir);
        ensureDirectory(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/api/uploads/profiles/" + filename;
    }

    @Override
    public String uploadMaintenancePhoto(String filename, MultipartFile file) throws IOException {
        Path dir = Paths.get(maintenanceUploadDir);
        ensureDirectory(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/maintenance/" + filename;
    }

    @Override
    public void deleteByUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        deleteIfLocalMapped(fileUrl, "/api/uploads/profiles/", profileUploadDir);
        deleteIfLocalMapped(fileUrl, "/uploads/maintenance/", maintenanceUploadDir);
    }

    private void ensureDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }

    private void deleteIfLocalMapped(String fileUrl, String publicPrefix, String localDir) {
        String normalized = fileUrl.trim();
        int prefixIndex = normalized.indexOf(publicPrefix);
        if (prefixIndex < 0) {
            return;
        }

        String filename = normalized.substring(prefixIndex + publicPrefix.length());
        if (filename.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get(localDir).resolve(filename));
        } catch (IOException ignored) {
            // Keep delete operation best-effort.
        }
    }
}

