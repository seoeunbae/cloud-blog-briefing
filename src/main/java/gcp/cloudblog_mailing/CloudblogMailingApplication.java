package gcp.cloudblog_mailing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration.class
})
@EnableScheduling
public class CloudblogMailingApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudblogMailingApplication.class, args);
    }
}
