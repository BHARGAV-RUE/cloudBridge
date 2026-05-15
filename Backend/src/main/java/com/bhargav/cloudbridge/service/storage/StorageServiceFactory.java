package com.bhargav.cloudbridge.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class StorageServiceFactory {

    private final Map<String, StorageService> registry;

    @Value("${cloudbridge.storage.primary-provider}")
    private String primaryProvider;

    @Value("${cloudbridge.storage.backup-provider}")
    private String backupProvider;

    public StorageServiceFactory(List<StorageService> allProviders) {
        this.registry = allProviders.stream()
                .collect(Collectors.toMap(
                        StorageService::getProviderName,
                        Function.identity()
                ));

        log.info("[StorageFactory] Registered Providers: {}", registry.keySet());
    }

    public StorageService getProvider(String providerName){
        StorageService service = registry.get(providerName);

        if(service == null) {
            throw new IllegalArgumentException(
                    "[StorageFactory] Unknown provider: '" + providerName + "'" + "Registered: " + registry.keySet()
            );
        }
        return service;
    }

    public StorageService getPrimaryProvider(){
        return getProvider(primaryProvider);
    }

    public List<String> getAvailableProviders() {
        return List.copyOf(registry.keySet());
    }
}