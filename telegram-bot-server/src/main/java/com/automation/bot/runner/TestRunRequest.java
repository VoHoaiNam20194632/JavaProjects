package com.automation.bot.runner;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO chứa thông tin cần thiết để chạy một test run.
 *
 * Tại sao dùng record/Builder pattern?
 * → Immutable: sau khi tạo thì không thể sửa — tránh race condition khi nhiều thread đọc.
 * → Builder: nhiều field optional (profile có thể null, testClass có thể null),
 *   builder rõ ràng hơn constructor 8 tham số.
 */
@Getter
@Builder
public class TestRunRequest {

    private final String runId;
    private final long chatId;
    private final long userId;
    private final String env;
    private final String profile;       // Maven profile: smoke, regression, api
    private final String testClass;     // Specific test class: LoginTest, DashboardTest
    private final String browser;
    private final boolean headless;
}
