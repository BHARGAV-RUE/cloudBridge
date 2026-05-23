package com.bhargav.cloudbridge.config;

import com.bhargav.cloudbridge.entity.CloudProvider;
import com.bhargav.cloudbridge.entity.CloudStorageCapacity;
import com.bhargav.cloudbridge.repository.CloudStorageCapacityRepository;
import com.bhargav.cloudbridge.util.StorageLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class StorageCapacityInitializer implements CommandLineRunner{

    private final CloudStorageCapacityRepository capacityRepository;

    @Override
    public void run(String... args){
        if(capacityRepository.count() > 0){
            log.info("[Initializer] Capacity records exist - skipping seed.");
        }

        List<CloudStorageCapacity> providers = List.of(

                CloudStorageCapacity.builder()
                        .provider(CloudProvider.AWS_S3)
                        .displayName("Amazon Web Services S3")
                        .totalLimitBytes(StorageLimits.AWS_LIMIT_BYTES)
                        .usedBytes(0L)
                        .isEnabled(true)
                        .build(),

                CloudStorageCapacity.builder()
                        .provider(CloudProvider.AZURE_BLOB)
                        .displayName("Microsoft Azure Blob Storage")
                        .totalLimitBytes(StorageLimits.AZURE_LIMIT_BYTES)
                        .usedBytes(0L)
                        .isEnabled(true)
                        .build(),

                CloudStorageCapacity.builder()
                        .provider(CloudProvider.ORACLE_CLOUD)
                        .displayName("Oracle Cloud Object Storage")
                        .totalLimitBytes(StorageLimits.ORACLE_LIMIT_BYTES)
                        .usedBytes(0L)
                        .isEnabled(true)
                        .build(),

                CloudStorageCapacity.builder()
                        .provider(CloudProvider.GOOGLE_DRIVE)
                        .displayName("Google Drive")
                        .totalLimitBytes(StorageLimits.GOOGLE_DRIVE_LIMIT_BYTES)
                        .usedBytes(0L)
                        .isEnabled(true)
                        .build()
        );

        capacityRepository.saveAll(providers);
        log.info("[Initializer] Seeded Capacity records for {} providers.", providers.size());
    }

}
