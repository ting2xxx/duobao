package com.javeme.duobao.controller;

import com.javeme.duobao.service.GcpStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final GcpStorageService gcpStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadImage(@RequestParam MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Invalid file");
        }

        String uploadedFile = gcpStorageService.uploadImage(file);

        if (uploadedFile == null) {

            throw new RuntimeException("Failed to upload image to GCP");
        }

        return ResponseEntity.ok(uploadedFile);
    }
}
