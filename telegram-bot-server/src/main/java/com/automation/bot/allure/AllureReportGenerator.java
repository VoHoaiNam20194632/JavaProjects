package com.automation.bot.allure;

import com.automation.bot.config.AllureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Chạy allure generate để tạo HTML report từ allure-results.
 *
 * Tại sao gọi allure CLI (process ngoài) thay vì dùng Allure Java API?
 * → Allure Java API chỉ SINH results (XML/JSON), không SINH report (HTML).
 * → Allure CLI (allure generate) mới tạo HTML report hoàn chỉnh.
 * → Dùng ProcessBuilder giống cách chạy mvn — nhất quán về approach.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AllureReportGenerator {

    private final AllureProperties allureProperties;

    /**
     * Chạy: allure generate allure-results -o allure-report --clean
     * @return URL đến report, hoặc null nếu generate fail
     */
    public String generateReport() {
        String allureExecutable = allureProperties.getAllureHome() + "/bin/allure.bat";
        String resultsDir = allureProperties.getResultsDir();
        String reportDir = allureProperties.getReportDir();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    allureExecutable,
                    "generate",
                    resultsDir,
                    "-o", reportDir,
                    "--clean"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Đọc output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[allure] {}", line);
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.error("Allure generate timed out");
                return null;
            }

            if (process.exitValue() != 0) {
                log.error("Allure generate failed with exit code: {}", process.exitValue());
                return null;
            }

            String reportUrl = allureProperties.getReportBaseUrl();
            log.info("Allure report generated: {}", reportUrl);
            return reportUrl;

        } catch (Exception e) {
            log.error("Failed to generate Allure report: {}", e.getMessage(), e);
            return null;
        }
    }
}
