package com.automation.bot.command.impl;

import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.command.BotCommand;
import com.automation.bot.runner.TestRunQueue;
import com.automation.bot.session.UserSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

/**
 * /status — Xem test đang chạy, queue, và env hiện tại.
 *
 * Hiển thị:
 * 1. Environment hiện tại của user
 * 2. Danh sách test đang RUNNING
 * 3. Danh sách test đang QUEUED
 * 4. Số slot trống
 */
@Component
@RequiredArgsConstructor
public class StatusCommand implements BotCommand {

    private final BotMessageSender messageSender;
    private final TestRunQueue testRunQueue;
    private final UserSessionManager sessionManager;

    @Override
    public String name() {
        return "status";
    }

    @Override
    public String description() {
        return "Show running tests, queue, and current environment";
    }

    @Override
    public void execute(Message message, String args) {
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        StringBuilder sb = new StringBuilder();
        sb.append("*Status*\n\n");

        // User's current environment
        sb.append("Your environment: `").append(sessionManager.getEnv(userId)).append("`\n\n");

        // Active runs
        List<TestRunQueue.TestRunInfo> allRuns = testRunQueue.getActiveRuns();

        if (allRuns.isEmpty()) {
            sb.append("No tests running or queued.\n");
        } else {
            long running = allRuns.stream()
                    .filter(r -> r.getStatus() == com.automation.bot.runner.RunStatus.RUNNING)
                    .count();
            long queued = allRuns.stream()
                    .filter(r -> r.getStatus() == com.automation.bot.runner.RunStatus.QUEUED)
                    .count();

            sb.append("Running: ").append(running).append(" | Queued: ").append(queued).append("\n\n");

            for (TestRunQueue.TestRunInfo info : allRuns) {
                var req = info.getRequest();
                String label = req.getProfile() != null ? req.getProfile() : req.getTestClass();
                String icon = info.getStatus() == com.automation.bot.runner.RunStatus.RUNNING ? "\u25b6\ufe0f" : "\u23f3";
                sb.append(icon).append(" `").append(req.getRunId()).append("` ")
                        .append(label).append(" (env=").append(req.getEnv()).append(")")
                        .append(" [").append(info.getStatus()).append("]\n");
            }
        }

        messageSender.send(chatId, sb.toString());
    }
}
