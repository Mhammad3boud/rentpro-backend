package com.rentpro.backend.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {
    String uploadProfilePicture(String filename, MultipartFile file) throws IOException;

    String uploadMaintenancePhoto(String filename, MultipartFile file) throws IOException;

    void deleteByUrl(String fileUrl);
}

