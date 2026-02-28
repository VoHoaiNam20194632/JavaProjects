package com.automation.bot.allure;

import com.automation.bot.config.AllureProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

/**
 * Push Allure HTML report lên branch gh-pages → GitHub Pages serve tại URL public.
 *
 * Approach: dùng git worktree để tạo working copy riêng cho branch gh-pages,
 * không ảnh hưởng đến branch chính (master) đang work.
 *
 * Flow:
 * 1. Tạo (hoặc reuse) git worktree cho gh-pages
 * 2. Xóa nội dung cũ trong worktree
 * 3. Copy allure-report vào worktree
 * 4. git add + commit + push
 * 5. Cleanup worktree
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubPagesPublisher {

    private final AllureProperties allureProperties;

    /**
     * Publish allure report lên gh-pages branch.
     * @return true nếu thành công
     */
    public boolean publish() {
        AllureProperties.GithubPages config = allureProperties.getGithubPages();
        if (!config.isEnabled()) {
            log.info("GitHub Pages publishing is disabled");
            return false;
        }

        String repoPath = config.getRepoPath();
        String reportDir = allureProperties.getReportDir();
        Path worktreePath = Paths.get(repoPath, ".gh-pages-worktree");

        try {
            // Đảm bảo branch gh-pages tồn tại
            ensureGhPagesBranch(repoPath);

            // Setup worktree
            setupWorktree(repoPath, worktreePath.toString());

            // Xóa nội dung cũ trong worktree (giữ .git)
            cleanWorktreeContent(worktreePath);

            // Copy allure report vào worktree
            copyDirectory(Paths.get(reportDir), worktreePath);

            // Commit và push
            commitAndPush(worktreePath.toString());

            log.info("Allure report published to GitHub Pages successfully");
            return true;

        } catch (Exception e) {
            log.error("Failed to publish Allure report to GitHub Pages: {}", e.getMessage(), e);
            return false;

        } finally {
            // Cleanup worktree
            removeWorktree(repoPath, worktreePath.toString());
        }
    }

    /**
     * Đảm bảo branch gh-pages tồn tại. Nếu chưa có → tạo mới từ remote hoặc tạo orphan.
     */
    private void ensureGhPagesBranch(String repoPath) throws Exception {
        // Kiểm tra branch gh-pages đã tồn tại local chưa
        int exitCode = runGit(repoPath, "git", "rev-parse", "--verify", "gh-pages");
        if (exitCode == 0) {
            log.debug("Branch gh-pages already exists locally");
            return;
        }

        // Kiểm tra trên remote
        exitCode = runGit(repoPath, "git", "rev-parse", "--verify", "origin/gh-pages");
        if (exitCode == 0) {
            runGitOrFail(repoPath, "git", "branch", "gh-pages", "origin/gh-pages");
            log.info("Created local gh-pages branch from origin/gh-pages");
            return;
        }

        // Tạo orphan branch gh-pages
        log.info("Creating orphan branch gh-pages...");
        Path tempDir = Files.createTempDirectory("gh-pages-init");
        try {
            runGitOrFail(repoPath, "git", "worktree", "add", "--detach", tempDir.toString());
            runGitOrFail(tempDir.toString(), "git", "checkout", "--orphan", "gh-pages");
            runGitOrFail(tempDir.toString(), "git", "rm", "-rf", ".");

            // Tạo file placeholder
            Files.writeString(tempDir.resolve("index.html"),
                    "<html><body><h1>Allure Reports</h1><p>Report will appear after first test run.</p></body></html>");

            runGitOrFail(tempDir.toString(), "git", "add", ".");
            runGitOrFail(tempDir.toString(), "git", "commit", "-m", "Initial gh-pages");
            runGitOrFail(tempDir.toString(), "git", "push", "origin", "gh-pages");
            log.info("Orphan branch gh-pages created and pushed");

        } finally {
            runGit(repoPath, "git", "worktree", "remove", "--force", tempDir.toString());
            deleteDirectoryQuietly(tempDir);
        }
    }

    private void setupWorktree(String repoPath, String worktreePath) throws Exception {
        // Xóa worktree cũ nếu còn sót
        Path wp = Paths.get(worktreePath);
        if (Files.exists(wp)) {
            runGit(repoPath, "git", "worktree", "remove", "--force", worktreePath);
            deleteDirectoryQuietly(wp);
        }

        runGitOrFail(repoPath, "git", "worktree", "add", worktreePath, "gh-pages");
        log.debug("Worktree created at {}", worktreePath);
    }

    private void cleanWorktreeContent(Path worktreePath) throws Exception {
        if (!Files.exists(worktreePath)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worktreePath)) {
            for (Path entry : stream) {
                // Giữ .git (worktree link)
                if (entry.getFileName().toString().equals(".git")) continue;
                if (Files.isDirectory(entry)) {
                    deleteDirectoryQuietly(entry);
                } else {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws Exception {
        if (!Files.exists(source)) {
            throw new IllegalStateException("Report directory does not exist: " + source);
        }

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws java.io.IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });

        log.debug("Copied report from {} to {}", source, target);
    }

    private void commitAndPush(String worktreePath) throws Exception {
        runGitOrFail(worktreePath, "git", "add", ".");

        // Kiểm tra có thay đổi không
        int diffExitCode = runGit(worktreePath, "git", "diff", "--cached", "--quiet");
        if (diffExitCode == 0) {
            log.info("No changes in report, skipping push");
            return;
        }

        String commitMsg = "Update Allure report - " + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        runGitOrFail(worktreePath, "git", "commit", "-m", commitMsg);
        runGitOrFail(worktreePath, "git", "push", "origin", "gh-pages");
        log.info("Report committed and pushed to gh-pages");
    }

    private void removeWorktree(String repoPath, String worktreePath) {
        try {
            runGit(repoPath, "git", "worktree", "remove", "--force", worktreePath);
        } catch (Exception e) {
            log.warn("Failed to remove worktree: {}", e.getMessage());
        }
    }

    private int runGit(String workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(new File(workDir))
                .redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[git] {}", line);
            }
        }

        boolean finished = process.waitFor(2, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git command timed out: " + String.join(" ", command));
        }

        return process.exitValue();
    }

    private void runGitOrFail(String workDir, String... command) throws Exception {
        int exitCode = runGit(workDir, command);
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed (exit=" + exitCode + "): " + String.join(" ", command));
        }
    }

    private void deleteDirectoryQuietly(Path dir) {
        try {
            if (!Files.exists(dir)) return;
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws java.io.IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, java.io.IOException exc) throws java.io.IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.warn("Failed to delete directory {}: {}", dir, e.getMessage());
        }
    }
}
