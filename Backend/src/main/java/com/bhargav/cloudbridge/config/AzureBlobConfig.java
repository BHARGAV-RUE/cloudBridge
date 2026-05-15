package com.bhargav.cloudbridge.config;

import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AzureBlobConfig {

    @Value("${azure.connection-string}")
    private String connectionString;

    @Value("${azure.container-name}")
    private String containerName;

    @Bean
    public BlobServiceClient blobServiceClient(){
        return new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    }

    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient){
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        if(!containerClient.exists()){
            containerClient.create();
            log.info("[Azure config] Container created: {}", containerName);
        }
        return containerClient;
    }
}
