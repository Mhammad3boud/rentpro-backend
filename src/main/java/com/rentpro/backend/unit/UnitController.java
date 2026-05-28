package com.rentpro.backend.unit;

import com.rentpro.backend.storage.StorageService;
import com.rentpro.backend.unit.dto.CreateUnitRequest;
import com.rentpro.backend.unit.dto.CreateUnitsBulkRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties/{propertyId}/units")
public class UnitController {

    private final UnitService unitService;
    private final UnitRepository unitRepository;
    private final UnitPhotoRepository unitPhotoRepository;
    private final StorageService storageService;

    public UnitController(UnitService unitService,
                          UnitRepository unitRepository,
                          UnitPhotoRepository unitPhotoRepository,
                          StorageService storageService) {
        this.unitService = unitService;
        this.unitRepository = unitRepository;
        this.unitPhotoRepository = unitPhotoRepository;
        this.storageService = storageService;
    }

    @PostMapping
    public Unit createUnit(@PathVariable UUID propertyId,
                           @RequestBody CreateUnitRequest request) {
        return unitService.createUnit(propertyId, request);
    }

    @PostMapping("/bulk")
    public List<Unit> createUnitsBulk(@PathVariable UUID propertyId,
                                      @RequestBody CreateUnitsBulkRequest request) {
        return unitService.createUnitsBulk(propertyId, request);
    }

    @GetMapping
    public List<Unit> getUnits(@PathVariable UUID propertyId) {
        return unitService.getUnitsByProperty(propertyId);
    }

    @GetMapping("/{unitId}/photos")
    public List<UnitPhoto> getUnitPhotos(@PathVariable UUID propertyId,
                                         @PathVariable UUID unitId) {
        return unitPhotoRepository.findByUnit_UnitId(unitId);
    }

    @PostMapping(value = "/{unitId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadUnitPhoto(@PathVariable UUID propertyId,
                                             @PathVariable UUID unitId,
                                             @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        boolean isImage = (contentType != null && contentType.startsWith("image/"))
                || originalName.endsWith(".jpg") || originalName.endsWith(".jpeg")
                || originalName.endsWith(".png") || originalName.endsWith(".webp") || originalName.endsWith(".heic");
        if (!isImage) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        try {
            String ext = resolveExtension(file.getOriginalFilename());
            String filename = unitId + "_" + System.currentTimeMillis() + ext;
            String url = storageService.uploadUnitImage(filename, file);

            UnitPhoto photo = new UnitPhoto();
            photo.setUnit(unit);
            photo.setImageUrl(url);
            UnitPhoto saved = unitPhotoRepository.save(photo);

            return ResponseEntity.ok(Map.of("photoId", saved.getPhotoId(), "imageUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deleteUnitPhoto(@PathVariable UUID propertyId,
                                                @PathVariable UUID photoId) {
        UnitPhoto photo = unitPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        storageService.deleteByUrl(photo.getImageUrl());
        unitPhotoRepository.delete(photo);
        return ResponseEntity.noContent().build();
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
