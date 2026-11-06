package com.sparesbilling.backend.feature;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class FeatureWebConfig implements WebMvcConfigurer {

    private final FeatureInterceptor featureInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureInterceptor).addPathPatterns("/api/**");
    }
}
