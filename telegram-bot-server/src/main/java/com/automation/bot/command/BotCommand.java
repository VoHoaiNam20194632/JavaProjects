package com.automation.bot.command;

import org.telegram.telegrambots.meta.api.objects.message.Message;

/**
 * Interface cho mọi bot command.
 *
 * Tại sao dùng interface thay vì abstract class?
 * → Interface định nghĩa CONTRACT (hợp đồng): mọi command phải có name, description, execute.
 * → Abstract class sẽ dùng cho AbstractTestCommand (shared logic chạy test) — đó mới là nơi đặt code chung.
 * → Tách biệt "cái gì phải làm" (interface) vs "làm chung như thế nào" (abstract class) = clean architecture.
 */
public interface BotCommand {

    /** Tên command (không có /), ví dụ: "smoke", "env", "help" */
    String name();

    /** Mô tả ngắn, hiển thị trong /help */
    String description();

    /** Xử lý command. args là phần sau tên command, ví dụ: "/smoke dev" → args = "dev" */
    void execute(Message message, String args);
}
