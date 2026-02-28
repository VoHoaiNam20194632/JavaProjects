package com.automation.bot.runner;

import com.automation.bot.config.TestRunnerProperties;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Quản lý thread pool chạy test — tối đa N test song song, vượt quá thì xếp hàng.
 *
 * Tại sao dùng ThreadPoolExecutor thay vì @Async?
 * → Kiểm soát chặt: biết chính xác bao nhiêu test đang chạy, bao nhiêu đang queue.
 * → @Async dùng global thread pool (SimpleAsyncTaskExecutor) — không control được max threads,
 *   và khó track active/queued tasks.
 * → Cần attach metadata (runId, userId, Process reference) vào mỗi task để cancel được.
 *
 * Tại sao max 3?
 * → Mỗi Chrome browser ~500MB-1GB RAM. 3 browsers = ~2-3GB. An toàn cho máy 16GB.
 * → Vượt quá 3 → queue chờ. User nhận message "Queued, position #N".
 */
@Slf4j
@Component
public class TestRunQueue {

    private final ExecutorService executor;
    private final int maxQueueSize;

    /** Track active runs để cancel và xem status */
    private final ConcurrentMap<String, TestRunInfo> activeRuns = new ConcurrentHashMap<>();

    @Getter
    public static class TestRunInfo {
        private final TestRunRequest request;
        private volatile Future<?> future;
        private volatile RunStatus status;
        private volatile Process process;

        public TestRunInfo(TestRunRequest request, Future<?> future) {
            this.request = request;
            this.future = future;
            this.status = RunStatus.QUEUED;
        }

        public void setFuture(Future<?> future) {
            this.future = future;
        }

        public void setStatus(RunStatus status) {
            this.status = status;
        }

        public void setProcess(Process process) {
            this.process = process;
        }
    }

    public TestRunQueue(TestRunnerProperties properties) {
        this.maxQueueSize = properties.getMaxQueueSize();
        int maxConcurrent = properties.getMaxConcurrentRuns();

        this.executor = new ThreadPoolExecutor(
                maxConcurrent,
                maxConcurrent,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(maxQueueSize),
                new ThreadPoolExecutor.AbortPolicy()
        );

        log.info("TestRunQueue initialized: maxConcurrent={}, maxQueue={}", maxConcurrent, maxQueueSize);
    }

    /**
     * Submit test run vào queue.
     *
     * @param request  thông tin test run
     * @param callback callback khi test hoàn tất (gửi kết quả về Telegram)
     * @return TestRunInfo để track, hoặc null nếu queue đầy
     */
    public TestRunInfo submit(TestRunRequest request, Consumer<TestRunInfo> callback) {
        try {
            TestRunInfo info = new TestRunInfo(request, null);
            activeRuns.put(request.getRunId(), info);

            Future<?> future = executor.submit(() -> {
                try {
                    callback.accept(info);
                } catch (Exception e) {
                    log.error("[{}] Error in test run callback: {}", request.getRunId(), e.getMessage(), e);
                }
            });

            info.setFuture(future);
            log.info("[{}] Test run submitted to queue", request.getRunId());
            return info;

        } catch (RejectedExecutionException e) {
            activeRuns.remove(request.getRunId());
            log.warn("[{}] Queue is full, rejecting request", request.getRunId());
            return null;
        }
    }

    /**
     * Cancel test run theo runId.
     * @return true nếu cancel thành công
     */
    public boolean cancel(String runId) {
        TestRunInfo info = activeRuns.get(runId);
        if (info == null) {
            return false;
        }

        info.setStatus(RunStatus.CANCELLED);

        // Kill process nếu đang chạy
        Process process = info.getProcess();
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            log.info("[{}] Process killed", runId);
        }

        // Cancel future
        info.getFuture().cancel(true);
        activeRuns.remove(runId);

        log.info("[{}] Test run cancelled", runId);
        return true;
    }

    /** Lấy tất cả active runs (đang chạy hoặc đang queue) */
    public List<TestRunInfo> getActiveRuns() {
        return new ArrayList<>(activeRuns.values());
    }

    /** Lấy active runs của một user cụ thể */
    public List<TestRunInfo> getRunsByUser(long userId) {
        return activeRuns.values().stream()
                .filter(info -> info.getRequest().getUserId() == userId)
                .toList();
    }

    /** Xóa run khỏi tracking (gọi sau khi hoàn tất) */
    public void removeRun(String runId) {
        activeRuns.remove(runId);
    }

    /** Graceful shutdown khi app tắt */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TestRunQueue...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
