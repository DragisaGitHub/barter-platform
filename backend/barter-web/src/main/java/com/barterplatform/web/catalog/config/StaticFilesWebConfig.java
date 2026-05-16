package com.barterplatform.web.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@Profile("legacy-local-static-files")
public class StaticFilesWebConfig implements WebMvcConfigurer {

    private final String basePath;

    public StaticFilesWebConfig(
            @Value("${barter.storage.local.base-path:./uploads}") String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(basePath).toAbsolutePath().normalize().toUri().toString();
        // Ensure trailing slash
        if (!absolutePath.endsWith("/")) {
            absolutePath = absolutePath + "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations(absolutePath);
    }
}

