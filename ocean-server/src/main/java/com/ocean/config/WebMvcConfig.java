package com.ocean.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC配置
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Value("${upload.avatar.dir:/data/ocean/uploads/avatars/}")
    private String avatarDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/uploads/**"
                );

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns(
                        "/api/user/**",
                        "/api/model/**",
                        "/api/alert/**",
                        "/api/announcement/**",
                        "/api/forecast/run/**",
                        "/api/forecast/trigger/**",
                        "/api/system/date/**"
                )
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/current"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = new File(avatarDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), avatarDir);
        }
        String location = "file:" + dir.getAbsolutePath().replace("\\", "/");
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(location);
    }
}
