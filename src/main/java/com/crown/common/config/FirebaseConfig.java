package com.crown.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseConfig {

    @Value("${firebase.credential.path:/root/app/firebase-service-account2.json}")
    private String credentialPath;

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(resolveCredential()))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }

    private InputStream resolveCredential() throws IOException {
        File file = new File(credentialPath);
        if (file.exists()) return new FileInputStream(file);
        InputStream is = getClass().getClassLoader().getResourceAsStream("firebase-service-account2.json");
        if (is != null) return is;
        throw new FileNotFoundException("Firebase credential not found: " + credentialPath);
    }
}
