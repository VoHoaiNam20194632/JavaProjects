package com.automation.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bot.allure")
public class AllureProperties {

    private String allureHome;
    private String reportDir;
    private String resultsDir;
    private String reportBaseUrl;

    private GithubPages githubPages = new GithubPages();

    @Getter
    @Setter
    public static class GithubPages {
        private boolean enabled = false;
        private String repoPath;
        private String baseUrl;
    }
}
