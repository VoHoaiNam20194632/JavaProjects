package com.automation.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bot.runner")
public class TestRunnerProperties {

    private String frameworkPath;
    private String mavenHome;
    private String defaultEnv = "dev";
    private String defaultBrowser = "chrome";
    private boolean headless = true;
    private int timeoutMinutes = 30;
    private int maxConcurrentRuns = 3;
    private int maxQueueSize = 5;
}
