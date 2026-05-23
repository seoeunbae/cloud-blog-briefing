package gcp.cloudblog_mailing.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("classpath:/key/cloudblog-mailing-e.json") 
    private Resource privateKey;

    @Bean
    public Firestore firestore() throws IOException {
        GoogleCredentials credentials;
        if (privateKey != null && privateKey.exists()) {
            log.info("Loading Firebase credentials from classpath key file.");
            try (InputStream is = privateKey.getInputStream()) {
                credentials = GoogleCredentials.fromStream(is);
            }
        } else {
            log.info("Firebase classpath key file not found. Falling back to Application Default Credentials (ADC) of GCP VM.");
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseApp firebaseApp;
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions firebaseOptions = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            firebaseApp = FirebaseApp.initializeApp(firebaseOptions);
        } else {
            firebaseApp = FirebaseApp.getInstance();
        }

        return FirestoreClient.getFirestore(firebaseApp);
    }
}