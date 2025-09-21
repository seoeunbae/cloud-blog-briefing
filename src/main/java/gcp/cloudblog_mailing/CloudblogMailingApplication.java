package gcp.cloudblog_mailing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudblogMailingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudblogMailingApplication.class, args);
    }
}
