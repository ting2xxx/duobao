package com.javeme.duobao.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class GcpStorageService {

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    public String uploadImage(MultipartFile file) {

        try{

            Storage storage = StorageOptions.getDefaultInstance().getService();

            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName != null ? originalFileName.substring(
                    originalFileName.lastIndexOf(".")) : ".jpg";
            String uniqueFileName = UUID.randomUUID().toString() + extension;

            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, uniqueFileName)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            return "https://storage.googleapis.com/" + bucketName + "/" + uniqueFileName;
        }catch (IOException e){
            throw new RuntimeException("Failed to upload image to GCP", e);
        }
    }
}
