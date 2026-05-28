package com.rentpro.backend.property;

import com.rentpro.backend.property.dto.CreatePropertyRequest;
import com.rentpro.backend.property.dto.PropertyWithUnitsDto;
import com.rentpro.backend.security.JwtUserContext;
import com.rentpro.backend.storage.StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final StorageService storageService;

    public PropertyController(PropertyService propertyService,
                              PropertyRepository propertyRepository,
                              PropertyPhotoRepository propertyPhotoRepository,
                              StorageService storageService) {
        this.propertyService = propertyService;
        this.propertyRepository = propertyRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.storageService = storageService;
    }

    @PostMapping("/{ownerId}")
    public Property createProperty(@PathVariable UUID ownerId,
                                   @RequestBody CreatePropertyRequest request) {
        return propertyService.createProperty(ownerId, request);
    }

    @GetMapping("/{ownerId}")
    public List<PropertyWithUnitsDto> getOwnerProperties(Authentication auth, @PathVariable UUID ownerId) {
        UUID authenticatedUserId = getOwnerId(auth);
        if (!authenticatedUserId.equals(ownerId)) {
            throw new RuntimeException("Unauthorized");
        }
        return propertyService.getOwnerPropertiesWithUnits(ownerId);
    }

    @GetMapping("/single/{propertyId}")
    public PropertyWithUnitsDto getPropertyById(Authentication auth, @PathVariable UUID propertyId) {
        UUID authenticatedUserId = getOwnerId(auth);
        return propertyService.getPropertyByIdAndOwner(propertyId, authenticatedUserId);
    }

    @PutMapping("/{propertyId}")
    public Property updateProperty(Authentication auth,
                                   @PathVariable UUID propertyId,
                                   @RequestBody CreatePropertyRequest request) {
        UUID ownerId = getOwnerId(auth);
        return propertyService.updateProperty(ownerId, propertyId, request);
    }

    @DeleteMapping("/{propertyId}")
    public void deleteProperty(Authentication auth, @PathVariable UUID propertyId) {
        UUID ownerId = getOwnerId(auth);
        propertyService.deleteProperty(ownerId, propertyId);
    }

    // List photos for a property
    @GetMapping("/{propertyId}/photos")
    public List<PropertyPhoto> getPhotos(@PathVariable UUID propertyId) {
        return propertyPhotoRepository.findByProperty_PropertyId(propertyId);
    }

    // Upload one photo for a property (call multiple times for multiple photos)
    @PostMapping(value = "/{propertyId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(Authentication auth,
                                         @PathVariable UUID propertyId,
                                         @RequestParam("file") MultipartFile file) {
        UUID ownerId = getOwnerId(auth);

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

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        if (!property.getOwner().getUserId().equals(ownerId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
        }

        try {
            String ext = resolveExtension(file.getOriginalFilename());
            String filename = propertyId + "_" + System.currentTimeMillis() + ext;
            String url = storageService.uploadPropertyImage(filename, file);

            PropertyPhoto photo = new PropertyPhoto();
            photo.setProperty(property);
            photo.setImageUrl(url);
            PropertyPhoto saved = propertyPhotoRepository.save(photo);

            return ResponseEntity.ok(Map.of("photoId", saved.getPhotoId(), "imageUrl", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
        }
    }

    // Delete a single property photo
    @DeleteMapping("/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(Authentication auth, @PathVariable UUID photoId) {
        UUID ownerId = getOwnerId(auth);

        PropertyPhoto photo = propertyPhotoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found"));

        if (!photo.getProperty().getOwner().getUserId().equals(ownerId)) {
            return ResponseEntity.status(403).build();
        }

        storageService.deleteByUrl(photo.getImageUrl());
        propertyPhotoRepository.delete(photo);
        return ResponseEntity.noContent().build();
    }

    private UUID getOwnerId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof JwtUserContext ctx)) {
            throw new RuntimeException("Unauthorized");
        }
        return UUID.fromString(ctx.userId());
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
