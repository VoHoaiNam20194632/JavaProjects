package com.automation.bot.session;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Lưu environment per userId — mỗi user có env riêng.
 *
 * Tại sao dùng ConcurrentHashMap thay vì HashMap + synchronized?
 * → ConcurrentHashMap lock ở segment level (không lock toàn bộ map),
 *   nên nhiều user gọi đồng thời không block nhau.
 * → HashMap + synchronized lock toàn bộ object → bottleneck khi nhiều user.
 * → Đây là in-memory storage: restart app thì mất. Chấp nhận được vì env chỉ là preference,
 *   user gõ /env dev lại mất 2 giây.
 */
@Component
public class UserSessionManager {

    private final ConcurrentMap<Long, String> userEnvironments = new ConcurrentHashMap<>();

    private static final String DEFAULT_ENV = "dev";

    public String getEnv(long userId) {
        return userEnvironments.getOrDefault(userId, DEFAULT_ENV);
    }

    public void setEnv(long userId, String env) {
        userEnvironments.put(userId, env.toLowerCase().trim());
    }

    public String getDefaultEnv() {
        return DEFAULT_ENV;
    }
}
