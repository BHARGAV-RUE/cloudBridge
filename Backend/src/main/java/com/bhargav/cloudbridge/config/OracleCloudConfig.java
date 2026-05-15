package com.bhargav.cloudbridge.config;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OracleCloudConfig {

    @Value("${oracle.config-path}")
    private String configPath;

    @Value("${oracle.region}")
    private String region;

    @Bean
    public ObjectStorageClient objectStorageClient() throws Exception {
        ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configPath, "DEFAULT");

        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

        ObjectStorageClient client = ObjectStorageClient.builder().region(region).build(provider);

        log.info("[oracle Config] ObjectStorageClient initialized | region: {}", region);

        return client;
    }
}
