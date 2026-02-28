package com.automation.bot.command;

/**
 * Parse raw text từ Telegram thành command name + arguments.
 *
 * Tại sao tách parser riêng thay vì parse trong AutomationBot?
 * → Single Responsibility: bot nhận message, parser parse, registry dispatch.
 * → Dễ unit test: test parse logic thuần túy, không cần mock Telegram.
 * → Xử lý edge cases ở 1 chỗ: "/smoke@BotName dev", leading/trailing spaces, etc.
 */
public class CommandParser {

    /** Kết quả parse: command name + arguments */
    public record ParsedCommand(String name, String args) {}

    /**
     * Parse "/smoke dev" → ParsedCommand("smoke", "dev")
     * Parse "/smoke@MyBot dev" → ParsedCommand("smoke", "dev")
     * Parse "hello" → null (không phải command)
     */
    public ParsedCommand parse(String text) {
        if (text == null || !text.startsWith("/")) {
            return null;
        }

        // Tách phần command và phần arguments
        String[] parts = text.split("\\s+", 2);
        String commandPart = parts[0].substring(1); // bỏ "/"
        String args = parts.length > 1 ? parts[1].trim() : "";

        // Xử lý trường hợp "/smoke@BotName" (Telegram gửi kèm @BotName trong group chat)
        int atIndex = commandPart.indexOf('@');
        if (atIndex > 0) {
            commandPart = commandPart.substring(0, atIndex);
        }

        return new ParsedCommand(commandPart.toLowerCase(), args);
    }
}
