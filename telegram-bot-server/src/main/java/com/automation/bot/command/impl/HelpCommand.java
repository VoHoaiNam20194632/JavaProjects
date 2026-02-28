package com.automation.bot.command.impl;

import com.automation.bot.bot.BotMessageSender;
import com.automation.bot.command.BotCommand;
import com.automation.bot.command.CommandRegistry;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Map;
import java.util.StringJoiner;

/**
 * /help — Liệt kê tất cả commands có sẵn.
 *
 * Tại sao inject CommandRegistry với @Lazy?
 * → Circular dependency: CommandRegistry inject List<BotCommand> (bao gồm HelpCommand),
 *   mà HelpCommand lại inject CommandRegistry.
 * → @Lazy giải quyết bằng cách tạo proxy — thực sự resolve khi gọi lần đầu,
 *   lúc đó cả 2 bean đã khởi tạo xong.
 * → Đây là pattern chuẩn Spring cho circular reference có chủ đích.
 */
@Component
public class HelpCommand implements BotCommand {

    private final BotMessageSender messageSender;
    private final CommandRegistry commandRegistry;

    public HelpCommand(BotMessageSender messageSender, @Lazy CommandRegistry commandRegistry) {
        this.messageSender = messageSender;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String description() {
        return "Show available commands";
    }

    @Override
    public void execute(Message message, String args) {
        Map<String, BotCommand> commands = commandRegistry.getAllCommands();

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("*Available Commands:*\n");

        for (Map.Entry<String, BotCommand> entry : commands.entrySet()) {
            joiner.add("/" + entry.getKey() + " — " + entry.getValue().description());
        }

        messageSender.send(message.getChatId(), joiner.toString());
    }
}
