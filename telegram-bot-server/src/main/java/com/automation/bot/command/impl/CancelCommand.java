package com.automation.bot.command.impl;

import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.command.BotCommand;
import com.automation.bot.runner.TestRunQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

/**
 * /cancel — Hủy test đang chạy của user hiện tại.
 *
 * Rules:
 * - User chỉ cancel được test của chính mình (theo userId).
 * - Nếu user có nhiều test, cancel tất cả. Hoặc cancel theo runId: /cancel abc12345
 * - Process bị kill bằng destroyForcibly() → Chrome + Maven chết ngay.
 */
@Component
@RequiredArgsConstructor
public class CancelCommand implements BotCommand {

    private final BotMessageSender messageSender;
    private final TestRunQueue testRunQueue;

    @Override
    public String name() {
        return "cancel";
    }

    @Override
    public String description() {
        return "Cancel your running test(s)";
    }

    @Override
    public void execute(Message message, String args) {
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        // Cancel theo runId cụ thể
        if (!args.isEmpty()) {
            boolean cancelled = testRunQueue.cancel(args.trim());
            if (cancelled) {
                messageSender.send(chatId, "Test `" + args.trim() + "` cancelled.");
            } else {
                messageSender.send(chatId, "Run ID `" + args.trim() + "` not found.");
            }
            return;
        }

        // Cancel tất cả runs của user
        List<TestRunQueue.TestRunInfo> userRuns = testRunQueue.getRunsByUser(userId);
        if (userRuns.isEmpty()) {
            messageSender.send(chatId, "You don't have any running tests.");
            return;
        }

        int cancelled = 0;
        for (TestRunQueue.TestRunInfo info : userRuns) {
            if (testRunQueue.cancel(info.getRequest().getRunId())) {
                cancelled++;
            }
        }

        messageSender.send(chatId, "Cancelled " + cancelled + " test run(s).");
    }
}
