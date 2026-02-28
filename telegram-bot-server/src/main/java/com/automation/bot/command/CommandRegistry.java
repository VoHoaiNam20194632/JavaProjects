package com.automation.bot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Auto-discover tất cả BotCommand beans và dispatch theo tên.
 *
 * Tại sao dùng Spring auto-discover thay vì register thủ công?
 * → Open/Closed Principle: thêm command mới chỉ cần tạo class implement BotCommand + @Component.
 *   Không cần sửa bất kỳ file nào khác — Spring tự inject vào List<BotCommand>.
 * → Tránh "shotgun surgery": không phải sửa nhiều file khi thêm/xóa command.
 *
 * LinkedHashMap giữ thứ tự insert → /help hiển thị commands theo đúng thứ tự được register.
 */
@Slf4j
@Component
public class CommandRegistry {

    private final Map<String, BotCommand> commands = new LinkedHashMap<>();
    private final CommandParser parser = new CommandParser();

    /**
     * Spring inject tất cả beans implement BotCommand vào đây.
     * Constructor injection đảm bảo registry sẵn sàng ngay khi bean được tạo.
     */
    public CommandRegistry(List<BotCommand> commandList) {
        for (BotCommand cmd : commandList) {
            commands.put(cmd.name(), cmd);
            log.info("Registered command: /{} - {}", cmd.name(), cmd.description());
        }
    }

    public BotCommand findCommand(String text) {
        CommandParser.ParsedCommand parsed = parser.parse(text);
        if (parsed == null) {
            return null;
        }
        return commands.get(parsed.name());
    }

    public CommandParser.ParsedCommand parse(String text) {
        return parser.parse(text);
    }

    public Map<String, BotCommand> getAllCommands() {
        return Collections.unmodifiableMap(commands);
    }
}
