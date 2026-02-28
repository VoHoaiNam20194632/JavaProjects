package com.automation.bot.security;

import com.automation.bot.config.BotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Whitelist filter: chỉ cho phép chat IDs được cấu hình trong application.yml.
 *
 * Tại sao cần whitelist?
 * → Bot chạy trên public Telegram — ai cũng có thể gửi message nếu biết bot username.
 * → Mà bot này trigger chạy test trên máy thật (ProcessBuilder) → PHẢI giới hạn ai được dùng.
 * → Defense in depth: filter ngay tầng đầu, trước khi command được parse.
 *
 * Tại sao tách thành class riêng thay vì check trong AutomationBot?
 * → Open/Closed Principle: sau này muốn thêm logic (rate limit, block user) chỉ sửa class này.
 * → Dễ test: inject mock ChatIdFilter vào AutomationBot khi unit test.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatIdFilter {

    private final BotProperties botProperties;

    public boolean isAllowed(long chatId) {
        boolean allowed = botProperties.getAllowedChatIds().contains(chatId);
        if (!allowed) {
            log.warn("Unauthorized access attempt from chatId={}", chatId);
        }
        return allowed;
    }
}
