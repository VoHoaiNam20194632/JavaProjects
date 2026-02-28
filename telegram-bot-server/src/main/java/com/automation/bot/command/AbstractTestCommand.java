package com.automation.bot.command;

import com.automation.bot.allure.AllureReportGenerator;
import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.config.TestRunnerProperties;
import com.automation.bot.notification.TelegramNotifier;
import com.automation.bot.parser.SurefireReportParser;
import com.automation.bot.parser.model.TestCase;
import com.automation.bot.parser.model.TestSuite;
import com.automation.bot.runner.*;
import com.automation.bot.session.UserSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;
import java.util.UUID;

/**
 * Base class cho tất cả commands chạy test (smoke, regression, api, login, dashboard).
 *
 * Tại sao dùng abstract class thay vì interface?
 * → Shared logic: resolve env, tạo TestRunRequest, submit vào queue, xử lý kết quả.
 *   Tất cả test commands đều làm giống nhau, chỉ khác profile/testClass.
 * → Template Method pattern: subclass chỉ override profile() và testClass(),
 *   flow chính (resolve env → build request → submit → notify) giữ nguyên.
 * → Giảm code duplication: nếu dùng interface, mỗi command phải copy-paste 30 dòng logic.
 */
@Slf4j
public abstract class AbstractTestCommand implements BotCommand {

    protected final BotMessageSender messageSender;
    protected final UserSessionManager sessionManager;
    protected final TestRunnerProperties runnerProperties;
    protected final TestRunner testRunner;
    protected final TestRunQueue testRunQueue;
    protected final TelegramNotifier notifier;
    protected final SurefireReportParser reportParser;
    protected final AllureReportGenerator allureGenerator;

    protected AbstractTestCommand(BotMessageSender messageSender,
                                  UserSessionManager sessionManager,
                                  TestRunnerProperties runnerProperties,
                                  TestRunner testRunner,
                                  TestRunQueue testRunQueue,
                                  TelegramNotifier notifier,
                                  SurefireReportParser reportParser,
                                  AllureReportGenerator allureGenerator) {
        this.messageSender = messageSender;
        this.sessionManager = sessionManager;
        this.runnerProperties = runnerProperties;
        this.testRunner = testRunner;
        this.testRunQueue = testRunQueue;
        this.notifier = notifier;
        this.reportParser = reportParser;
        this.allureGenerator = allureGenerator;
    }

    /** Maven profile cho command này (ví dụ: "smoke", "regression"). Null nếu chạy theo testClass */
    protected abstract String profile();

    /** Test class cụ thể (ví dụ: "LoginTest"). Null nếu chạy theo profile */
    protected abstract String testClass();

    @Override
    public void execute(Message message, String args) {
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        // Resolve env: args override > saved env > default
        String env = resolveEnv(userId, args);
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String label = profile() != null ? profile() : testClass();

        TestRunRequest request = TestRunRequest.builder()
                .runId(runId)
                .chatId(chatId)
                .userId(userId)
                .env(env)
                .profile(profile())
                .testClass(testClass())
                .browser(runnerProperties.getDefaultBrowser())
                .headless(runnerProperties.isHeadless())
                .build();

        // Submit vào queue
        TestRunQueue.TestRunInfo runInfo = testRunQueue.submit(request, info -> {
            executeTestRun(info);
        });

        if (runInfo == null) {
            notifier.notifyQueueFull(chatId);
            return;
        }

        notifier.notifyQueued(chatId, runId, label, env);
    }

    /**
     * Thực thi test run — method này chạy trên worker thread của TestRunQueue.
     * Flow: set RUNNING → chạy mvn → parse Surefire XML → notify kết quả → cleanup
     */
    private void executeTestRun(TestRunQueue.TestRunInfo info) {
        TestRunRequest request = info.getRequest();
        String runId = request.getRunId();
        String label = request.getProfile() != null ? request.getProfile() : request.getTestClass();

        try {
            info.setStatus(RunStatus.RUNNING);
            notifier.notifyRunning(request.getChatId(), label, request.getEnv());

            // Chạy Maven test
            TestRunResult rawResult = testRunner.run(request);
            info.setStatus(rawResult.getStatus());

            // Generate Allure report
            String allureUrl = allureGenerator.generateReport();

            // Parse Surefire XML để lấy chi tiết pass/fail
            List<TestSuite> suites = reportParser.parseReports(runnerProperties.getFrameworkPath());
            List<TestCase> failedTests = reportParser.getFailedTests(suites);

            TestRunResult enrichedResult = reportParser.buildResult(
                    runId, suites, rawResult.getDuration(), allureUrl);

            // Gửi kết quả đẹp về Telegram
            notifier.notifyResult(request, enrichedResult, failedTests);

        } catch (Exception e) {
            log.error("[{}] Error executing test run: {}", runId, e.getMessage(), e);
            info.setStatus(RunStatus.FAILED);
            notifier.notifyError(request.getChatId(), label, e.getMessage());

        } finally {
            testRunQueue.removeRun(runId);
        }
    }

    /**
     * Resolve env: nếu user gõ "/smoke prod" → dùng prod (override lần này).
     * Nếu gõ "/smoke" → dùng env đã save. Nếu chưa save → default "dev".
     */
    private String resolveEnv(long userId, String args) {
        if (args != null && !args.isBlank()) {
            return args.trim().toLowerCase();
        }
        return sessionManager.getEnv(userId);
    }
}
