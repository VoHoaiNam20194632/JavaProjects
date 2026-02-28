package com.automation.bot.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Wrapper quanh TelegramClient để gửi message.
 *
 * Tại sao tách ra thay vì gọi TelegramClient trực tiếp?
 * → Single Responsibility: Bot class lo nhận message, class này lo gửi.
 * → Dễ mock trong test (mock BotMessageSender thay vì mock TelegramClient).
 * → Centralize error handling: mọi chỗ gửi message đều đi qua đây.
 */
@Slf4j
@Component
public class BotMessageSender {

    private final TelegramClient telegramClient;

    public BotMessageSender(com.automation.bot.config.BotProperties botProperties) {
        this.telegramClient = new OkHttpTelegramClient(botProperties.getToken());
    }

    public void send(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId={}: {}", chatId, e.getMessage(), e);
        }
    }

    public void sendHtml(long chatId, String html) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(html)
                .parseMode("HTML")
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send HTML message to chatId={}: {}", chatId, e.getMessage(), e);
        }
    }
}
