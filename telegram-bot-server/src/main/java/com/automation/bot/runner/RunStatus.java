package com.automation.bot.runner;

/**
 * Trạng thái của một test run.
 */
public enum RunStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
