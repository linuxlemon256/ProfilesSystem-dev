package com.example.profilessystem.config;

import com.example.profilessystem.auth.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/learnServer.html", "/account.html", "/LLMindex.html", "/projectDemo.html", "/storage.html", "/groups.html", "/communication.html", "/notification.html", "/feedback.html", "/joinUs.html", "/managerPanel.html")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/login/password",
                        "/api/user/login/email",
                        "/api/user/register",
                        "/api/manager/login",
                        "/api/ping");
    }
}
