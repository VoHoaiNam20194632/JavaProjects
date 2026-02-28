package com.automation.bot.command.impl;

import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.command.BotCommand;
import com.automation.bot.session.UserSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * /env <name> — Set environment cho user hiện tại.
 *
 * Flow:
 * 1. User gõ /env dev → bot nhớ env=dev cho user này
 * 2. User gõ /smoke → bot dùng env=dev (đã lưu)
 * 3. User gõ /smoke prod → override env=prod cho lần này, KHÔNG đổi saved env
 *
 * Tại sao validate env?
 * → Tránh user gõ sai (ví dụ: /env abc) rồi chạy test fail vì env không tồn tại.
 * → Fail fast tốt hơn fail silent: thông báo ngay thay vì để test chạy rồi mới lỗi.
 */
@Component
@RequiredArgsConstructor
public class EnvCommand implements BotCommand {

    private static final java.util.Set<String> VALID_ENVS = java.util.Set.of("dev", "staging", "prod");

    private final BotMessageSender messageSender;
    private final UserSessionManager sessionManager;

    @Override
    public String name() {
        return "env";
    }

    @Override
    public String description() {
        return "Set environment (dev/staging/prod)";
    }

    @Override
    public void execute(Message message, String args) {
        long chatId = message.getChatId();
        long userId = message.getFrom().getId();

        if (args.isEmpty()) {
            String currentEnv = sessionManager.getEnv(userId);
            messageSender.send(chatId,
                    "Current environment: *" + currentEnv + "*\n" +
                    "Usage: /env <dev|staging|prod>");
            return;
        }

        String env = args.toLowerCase().trim();
        if (!VALID_ENVS.contains(env)) {
            messageSender.send(chatId,
                    "Invalid environment: " + env + "\n" +
                    "Valid options: dev, staging, prod");
            return;
        }

        sessionManager.setEnv(userId, env);
        messageSender.send(chatId, "Environment set to: *" + env + "*");
    }
}
