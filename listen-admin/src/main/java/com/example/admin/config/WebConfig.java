package com.example.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${userFile.path}")
    private String userFilePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /userFile/* 到本地文件路径
        // 例如: http://localhost:8081/userFile/ZJZ100861776134586477/2.wav -> C:/Users/86182/Desktop/work/listen/userFile/ZJZ100861776134586477/2.wav
        String basePath = userFilePath.replace("\\", "/");
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        
        registry.addResourceHandler("/userFile/**")
                .addResourceLocations("file:" + basePath);
    }
}
