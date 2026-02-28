package com.automation.bot.parser;

import com.automation.bot.parser.model.TestCase;
import com.automation.bot.parser.model.TestSuite;
import com.automation.bot.runner.RunStatus;
import com.automation.bot.runner.TestRunResult;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse Surefire XML reports từ target/surefire-reports/ directory.
 *
 * Tại sao parse XML thay vì đọc console output?
 * → Console output format thay đổi giữa Surefire versions → fragile parsing.
 * → XML là structured data, có schema chuẩn → reliable parsing.
 * → Có đầy đủ thông tin: tên test, thời gian, failure message, stacktrace.
 * → Allure cũng đọc từ XML/JSON, không từ console.
 */
@Slf4j
@Component
public class SurefireReportParser {

    private final XmlMapper xmlMapper = new XmlMapper();

    /**
     * Parse tất cả TEST-*.xml files trong surefire-reports directory.
     * Mỗi test class tạo ra 1 file XML riêng.
     */
    public List<TestSuite> parseReports(String frameworkPath) {
        File reportsDir = new File(frameworkPath, "target/surefire-reports");
        List<TestSuite> suites = new ArrayList<>();

        if (!reportsDir.exists() || !reportsDir.isDirectory()) {
            log.warn("Surefire reports directory not found: {}", reportsDir.getAbsolutePath());
            return suites;
        }

        File[] xmlFiles = reportsDir.listFiles((dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) {
            log.warn("No Surefire XML reports found in: {}", reportsDir.getAbsolutePath());
            return suites;
        }

        for (File xmlFile : xmlFiles) {
            try {
                TestSuite suite = xmlMapper.readValue(xmlFile, TestSuite.class);
                suites.add(suite);
                log.debug("Parsed {}: tests={}, failures={}, errors={}", xmlFile.getName(),
                        suite.getTests(), suite.getFailures(), suite.getErrors());
            } catch (Exception e) {
                log.error("Failed to parse {}: {}", xmlFile.getName(), e.getMessage());
            }
        }

        return suites;
    }

    /**
     * Aggregate kết quả từ nhiều TestSuite → 1 TestRunResult.
     */
    public TestRunResult buildResult(String runId, List<TestSuite> suites, Duration duration, String allureUrl) {
        int totalTests = 0, totalFailed = 0, totalErrors = 0, totalSkipped = 0;

        for (TestSuite suite : suites) {
            totalTests += suite.getTests();
            totalFailed += suite.getFailures();
            totalErrors += suite.getErrors();
            totalSkipped += suite.getSkipped();
        }

        int totalPassed = totalTests - totalFailed - totalErrors - totalSkipped;
        boolean allPassed = totalFailed == 0 && totalErrors == 0;

        return TestRunResult.builder()
                .runId(runId)
                .status(allPassed ? RunStatus.COMPLETED : RunStatus.FAILED)
                .totalTests(totalTests)
                .passed(totalPassed)
                .failed(totalFailed)
                .errors(totalErrors)
                .skipped(totalSkipped)
                .duration(duration)
                .allureReportUrl(allureUrl)
                .build();
    }

    /**
     * Lấy danh sách test cases bị fail (để hiện chi tiết trên Telegram).
     */
    public List<TestCase> getFailedTests(List<TestSuite> suites) {
        List<TestCase> failedTests = new ArrayList<>();
        for (TestSuite suite : suites) {
            if (suite.getTestCases() != null) {
                for (TestCase tc : suite.getTestCases()) {
                    if (tc.isFailed() || tc.isError()) {
                        failedTests.add(tc);
                    }
                }
            }
        }
        return failedTests;
    }
}
