package com.bhargav.cloudbridge.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

@Configuration
@Slf4j
public class GoogleConfig {

    @Value("${google.drive.credentials-path}")
    private String credentialsPath;

    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";

    @Bean
    public Drive googleDriveClient() throws Exception{
        GoogleCredentials credentials;

        try(InputStream is = new FileInputStream(credentialsPath)){
            credentials = GoogleCredentials.fromStream(is).createScoped(Collections.singleton(DRIVE_SCOPE));

            log.info("[Google Drive Config] Credentials loaded from: {}", credentialsPath );

            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CloudBridge").build();
        }
    }
}
