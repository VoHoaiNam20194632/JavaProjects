package com.automation.bot.allure;

import com.automation.bot.config.AllureProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve Allure HTML report qua Spring MVC tại /allure/**
 *
 * Tại sao dùng ResourceHandler thay vì viết controller đọc file?
 * → ResourceHandler là cách chuẩn Spring MVC serve static files.
 * → Tự xử lý Content-Type, caching, 304 Not Modified, directory listing.
 * → Performance tốt hơn vì delegate cho Servlet container (Tomcat).
 * → An toàn: chỉ serve files trong directory chỉ định, không bị path traversal.
 *
 * Ví dụ: http://localhost:8080/allure/index.html
 *       → maps to D:/JavaProjects/automation-framework/allure-report/index.html
 */
@Configuration
public class AllureReportController implements WebMvcConfigurer {

    private final AllureProperties allureProperties;

    public AllureReportController(AllureProperties allureProperties) {
        this.allureProperties = allureProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String reportDir = allureProperties.getReportDir();
        // Đảm bảo path kết thúc bằng /
        if (!reportDir.endsWith("/")) {
            reportDir += "/";
        }

        registry.addResourceHandler("/allure/**")
                .addResourceLocations("file:" + reportDir)
                .setCachePeriod(0); // Không cache — luôn serve version mới nhất
    }
}
