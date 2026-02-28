package com.automation.bot.bot;

import com.automation.bot.command.BotCommand;
import com.automation.bot.command.CommandParser;
import com.automation.bot.command.CommandRegistry;
import com.automation.bot.config.BotProperties;
import com.automation.bot.security.ChatIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * Main Telegram bot class — nhận mọi Update từ Telegram qua Long Polling.
 *
 * Tại sao implement cả SpringLongPollingBot VÀ LongPollingSingleThreadUpdateConsumer?
 * → SpringLongPollingBot: để Spring Boot auto-config tự register bot.
 * → LongPollingSingleThreadUpdateConsumer: xử lý từng Update một (thay vì List<Update>),
 *   code sạch hơn và tránh race condition khi xử lý command.
 *
 * Flow: Telegram API → Long Polling → consume(List<Update>) → consume(Update) → processMessage()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final BotProperties botProperties;
    private final BotMessageSender messageSender;
    private final ChatIdFilter chatIdFilter;
    private final CommandRegistry commandRegistry;

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        // Trả về chính mình vì class này implement LongPollingSingleThreadUpdateConsumer
        return this;
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        if (!message.hasText()) {
            return;
        }

        String text = message.getText().trim();
        long chatId = message.getChatId();

        // Security check — chặn ngay nếu chatId không nằm trong whitelist
        if (!chatIdFilter.isAllowed(chatId)) {
            return;
        }

        log.info("Received message from chatId={}: {}", chatId, text);

        // Dispatch command qua CommandRegistry
        BotCommand command = commandRegistry.findCommand(text);
        if (command == null) {
            messageSender.send(chatId, "Unknown command. Type /help to see available commands.");
            return;
        }

        CommandParser.ParsedCommand parsed = commandRegistry.parse(text);
        command.execute(message, parsed.args());
    }

    @AfterBotRegistration
    public void afterRegistration() {
        log.info("Telegram Bot '{}' registered and running!", botProperties.getUsername());
    }
}
