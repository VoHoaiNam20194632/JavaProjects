package com.automation.bot.runner;

import com.automation.bot.config.TestRunnerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Chạy Maven test bằng ProcessBuilder.
 *
 * Tại sao dùng ProcessBuilder thay vì gọi TestNG/JUnit programmatically?
 * → Isolation hoàn toàn: test framework chạy trong process riêng, classpath riêng.
 *   Nếu test crash (OOM, segfault) → chỉ chết process con, bot server vẫn sống.
 * → Không conflict classpath: bot dùng Spring Boot 3.4, framework có thể dùng version khác.
 * → Giống cách CI/CD chạy (mvn test) → kết quả nhất quán.
 *
 * Tại sao dùng mvn.cmd trên Windows?
 * → Windows không có mvn executable, chỉ có mvn.cmd (batch file).
 * → ProcessBuilder trên Windows không tự tìm .cmd, phải chỉ đường dẫn đầy đủ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestRunner {

    private final TestRunnerProperties properties;

    /**
     * Chạy Maven test command và trả về exit code.
     * Method này blocking — gọi từ TestRunQueue (thread pool) để không block main thread.
     */
    public TestRunResult run(TestRunRequest request) {
        Instant start = Instant.now();
        String runId = request.getRunId();

        try {
            List<String> command = buildCommand(request);
            log.info("[{}] Executing: {}", runId, String.join(" ", command));

            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .directory(new File(properties.getFrameworkPath()))
                    .redirectErrorStream(true);

            // Set JAVA_HOME, MAVEN_HOME nếu cần
            processBuilder.environment().put("MAVEN_HOME", properties.getMavenHome());

            Process process = processBuilder.start();

            // Đọc output để tránh buffer đầy → process bị block (deadlock)
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("[{}] {}", runId, line);
                }
            }

            boolean finished = process.waitFor(properties.getTimeoutMinutes(), TimeUnit.MINUTES);
            Duration duration = Duration.between(start, Instant.now());

            if (!finished) {
                process.destroyForcibly();
                log.error("[{}] Test run timed out after {} minutes", runId, properties.getTimeoutMinutes());
                return TestRunResult.builder()
                        .runId(runId)
                        .status(RunStatus.FAILED)
                        .duration(duration)
                        .errorMessage("Timeout after " + properties.getTimeoutMinutes() + " minutes")
                        .build();
            }

            int exitCode = process.exitValue();
            log.info("[{}] Process exited with code={}, duration={}", runId, exitCode, duration);

            // exitCode 0 = success, khác 0 = có test fail hoặc build error
            return TestRunResult.builder()
                    .runId(runId)
                    .status(exitCode == 0 ? RunStatus.COMPLETED : RunStatus.FAILED)
                    .duration(duration)
                    .errorMessage(exitCode != 0 ? "Exit code: " + exitCode : null)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Failed to execute test run: {}", runId, e.getMessage(), e);
            return TestRunResult.builder()
                    .runId(runId)
                    .status(RunStatus.FAILED)
                    .duration(Duration.between(start, Instant.now()))
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * Build Maven command: mvn.cmd test -Psmoke -Denv=dev -Dbrowser=chrome -Dheadless=true
     */
    private List<String> buildCommand(TestRunRequest request) {
        List<String> command = new ArrayList<>();

        // mvn.cmd trên Windows
        String mvnExecutable = properties.getMavenHome() + "/bin/mvn.cmd";
        command.add(mvnExecutable);
        command.add("test");

        // Maven profile (smoke, regression, api)
        if (request.getProfile() != null && !request.getProfile().isEmpty()) {
            command.add("-P" + request.getProfile());
        }

        // Specific test class
        if (request.getTestClass() != null && !request.getTestClass().isEmpty()) {
            command.add("-Dtest=" + request.getTestClass());
        }

        // Environment, browser, headless
        command.add("-Denv=" + request.getEnv());
        command.add("-Dbrowser=" + request.getBrowser());
        command.add("-Dheadless=" + request.isHeadless());

        // Không cần build lại, chỉ chạy test
        command.add("-Dsurefire.useFile=false");

        return command;
    }

    /** Cho phép cancel bằng Process.destroyForcibly() từ bên ngoài */
    public Process startProcess(TestRunRequest request) throws Exception {
        List<String> command = buildCommand(request);
        log.info("[{}] Starting process: {}", request.getRunId(), String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(new File(properties.getFrameworkPath()))
                .redirectErrorStream(true);
        processBuilder.environment().put("MAVEN_HOME", properties.getMavenHome());

        return processBuilder.start();
    }
}
