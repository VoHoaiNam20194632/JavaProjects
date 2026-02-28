package com.automation.bot.notification;

import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.parser.model.TestCase;
import com.automation.bot.runner.RunStatus;
import com.automation.bot.runner.TestRunRequest;
import com.automation.bot.runner.TestRunResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Format và gửi kết quả test đẹp mắt về Telegram.
 *
 * Tại sao tách riêng thay vì format trong AbstractTestCommand?
 * → Single Responsibility: command lo logic chạy test, notifier lo format message.
 * → Sau này muốn gửi thêm Slack/Email chỉ cần thêm notifier mới, không sửa command.
 * → Dễ thay đổi format mà không ảnh hưởng flow chạy test.
 */
@Component
@RequiredArgsConstructor
public class TelegramNotifier {

    private final BotMessageSender messageSender;

    public void notifyResult(TestRunRequest request, TestRunResult result, List<TestCase> failedTests) {
        StringBuilder sb = new StringBuilder();

        // Header với status icon
        String icon = result.getStatus() == RunStatus.COMPLETED ? "\u2705" : "\u274c";
        String label = request.getProfile() != null ? request.getProfile() : request.getTestClass();
        sb.append(icon).append(" *").append(label.toUpperCase()).append(" TEST RESULT*\n\n");

        // Summary
        sb.append("Environment: `").append(request.getEnv()).append("`\n");
        sb.append("Duration: ").append(formatDuration(result.getDuration())).append("\n\n");

        // Counts
        sb.append("Total: ").append(result.getTotalTests()).append("\n");
        sb.append("\u2705 Passed: ").append(result.getPassed()).append("\n");
        sb.append("\u274c Failed: ").append(result.getFailed()).append("\n");

        if (result.getErrors() > 0) {
            sb.append("\u26a0\ufe0f Errors: ").append(result.getErrors()).append("\n");
        }
        if (result.getSkipped() > 0) {
            sb.append("\u23ed Skipped: ").append(result.getSkipped()).append("\n");
        }

        // Failed test details (tối đa 5)
        if (failedTests != null && !failedTests.isEmpty()) {
            sb.append("\n*Failed Tests:*\n");
            int limit = Math.min(failedTests.size(), 5);
            for (int i = 0; i < limit; i++) {
                TestCase tc = failedTests.get(i);
                sb.append("  \u2022 ").append(tc.getName());
                if (tc.getFailure() != null && tc.getFailure().getMessage() != null) {
                    String msg = tc.getFailure().getMessage();
                    // Truncate message dài
                    if (msg.length() > 80) {
                        msg = msg.substring(0, 80) + "...";
                    }
                    sb.append("\n    _").append(msg).append("_");
                }
                sb.append("\n");
            }
            if (failedTests.size() > 5) {
                sb.append("  ... and ").append(failedTests.size() - 5).append(" more\n");
            }
        }

        // Allure report link
        if (result.getAllureReportUrl() != null) {
            sb.append("\n\ud83d\udcca [View Allure Report](").append(result.getAllureReportUrl()).append(")");
        }

        messageSender.send(request.getChatId(), sb.toString());
    }

    public void notifyQueued(long chatId, String runId, String label, String env) {
        messageSender.send(chatId,
                "\u23f3 *Test Queued*\n" +
                "Suite: " + label + "\n" +
                "Environment: `" + env + "`\n" +
                "Run ID: `" + runId + "`\n\n" +
                "Use /status to check progress.");
    }

    public void notifyRunning(long chatId, String label, String env) {
        messageSender.send(chatId,
                "\u25b6\ufe0f Running: *" + label + "* (env=" + env + ") ...");
    }

    public void notifyError(long chatId, String label, String errorMessage) {
        messageSender.send(chatId,
                "\u274c *" + label + "* failed to start\n" +
                "Error: " + errorMessage);
    }

    public void notifyQueueFull(long chatId) {
        messageSender.send(chatId,
                "\u26d4 Queue is full! Please wait for current tests to finish.\n" +
                "Use /status to check running tests.");
    }

    private String formatDuration(java.time.Duration duration) {
        if (duration == null) return "N/A";
        long minutes = duration.toMinutes();
        long seconds = duration.toSecondsPart();
        return minutes > 0 ? minutes + "m " + seconds + "s" : seconds + "s";
    }
}
