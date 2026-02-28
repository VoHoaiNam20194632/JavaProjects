package com.automation.bot.runner;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;

/**
 * DTO chứa kết quả sau khi test run hoàn tất.
 */
@Getter
@Builder
public class TestRunResult {

    private final String runId;
    private final RunStatus status;
    private final int totalTests;
    private final int passed;
    private final int failed;
    private final int skipped;
    private final int errors;
    private final Duration duration;
    private final String errorMessage;      // Nếu process bị crash hoặc timeout
    private final String allureReportUrl;   // Link đến report
}
