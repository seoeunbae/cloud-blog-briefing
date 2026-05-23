package gcp.cloudblog_mailing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /docs/** request path to the external 'docs' directory in the project root
        registry.addResourceHandler("/docs/**")
                .addResourceLocations("file:./docs/")
                .setCachePeriod(0); // Disable caching for development
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect /docs to /docs/index.html to ensure clean landing
        registry.addRedirectViewController("/docs", "/docs/index.html");
        registry.addRedirectViewController("/docs/", "/docs/index.html");
    }
}
