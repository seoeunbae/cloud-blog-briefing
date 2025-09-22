package gcp.cloudblog_mailing.controller;

import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class HealthcheckController {
    private final Firestore firestore;

    @GetMapping("/firebase-connection")
    public String testConnection() {
        try {
            // 간단한 Firestore 작업 테스트
            firestore.collection("test").document("test").set(Map.of("test", "test")).get();
            return "Firebase connection successful!";
        } catch (Exception e) {
            return "Firebase connection failed: " + e.getMessage();
        }
    }

}
