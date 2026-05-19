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
            //Authenticate and get the default GCP storage client instance
            Storage storage = StorageOptions.getDefaultInstance().getService();
            //get the file original file name
            String originalFileName = file.getOriginalFilename();
            //get the file extension, index after .
            String extension = originalFileName != null ? originalFileName.substring(
                    originalFileName.lastIndexOf(".")) : ".jpg";
            //generate a unique file name
            String uniqueFileName = UUID.randomUUID().toString() + extension;


            //File or folder is just a blob of data sitting in a GCP bucket
            //create a blobInfo with bucketName and unique file name
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, uniqueFileName)
                    .setContentType(file.getContentType())//set the content type
                    .build();
            //upload the file to GCP
            storage.create(blobInfo, file.getBytes());

            //return the URL of the uploaded image
            return "https://storage.googleapis.com/" + bucketName + "/" + uniqueFileName;
        }catch (IOException e){
            throw new RuntimeException("Failed to upload image to GCP", e);
        }
    }
}
