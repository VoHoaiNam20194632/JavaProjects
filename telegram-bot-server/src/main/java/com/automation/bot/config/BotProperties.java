package com.automation.bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Telegram bot configuration từ application.yml.
 *
 * Tại sao dùng @ConfigurationProperties thay vì @Value?
 * → Type-safe binding: Spring tự map nested YAML vào object.
 * → IDE auto-complete, validation, và dễ test hơn @Value("${bot.telegram.token}").
 * → Khi config phình ra (thêm field), chỉ cần thêm field vào class — không cần sửa constructor.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bot.telegram")
public class BotProperties {

    private String token;
    private String username;
    private List<Long> allowedChatIds = new ArrayList<>();
}
