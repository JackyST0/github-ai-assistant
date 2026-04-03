package com.github.ai.assistant.cli;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.service.ReviewService;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * PR 审查命令
 * 
 * 使用 AI 审查 GitHub Pull Request
 */
@Component
@Command(
    name = "review",
    description = "AI 智能审查 Pull Request",
    mixinStandardHelpOptions = true
)
public class ReviewCommand implements Callable<Integer> {

    private final ReviewService reviewService;

    @Option(names = {"--pr"}, description = "PR 编号", required = true)
    private int prNumber;

    @Option(names = {"-r", "--repo"}, description = "仓库名 (格式: owner/repo)")
    private String repository;

    @Option(names = {"--comment"}, description = "自动发布评论到 GitHub", defaultValue = "false")
    private boolean autoComment;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)，默认使用应用配置")
    private String model;

    @Option(names = {"--focus"}, description = "审查重点 (security/performance/style/all)", defaultValue = "all")
    private String focus;

    public ReviewCommand(ReviewService reviewService, AppConfig appConfig) {
        this.reviewService = reviewService;
        this.model = appConfig.getAi().getDefaultModel();
    }

    @Override
    public Integer call() {
        try {
            var result = ConsoleUtils.withSpinner("🔍 正在审查 PR #" + prNumber + "...",
                () -> reviewService.reviewPullRequest(repository, prNumber, focus, model));
            
            ConsoleUtils.doubleSection("📊 PR 审查结果");
            ConsoleUtils.line(result.summary());
            ConsoleUtils.blankLine();
            
            if (!result.issues().isEmpty()) {
                ConsoleUtils.line("⚠️ 发现的问题：");
                result.issues().forEach(ConsoleUtils::bullet);
                ConsoleUtils.blankLine();
            }
            
            if (!result.suggestions().isEmpty()) {
                ConsoleUtils.line("💡 改进建议：");
                result.suggestions().forEach(ConsoleUtils::bullet);
            }
            
            ConsoleUtils.doubleSeparator();
            ConsoleUtils.line("评分: " + result.score() + "/100");
            
            if (autoComment) {
                ConsoleUtils.blankLine();
                ConsoleUtils.line("📤 正在发布评论到 GitHub...");
                reviewService.postReviewComment(repository, prNumber, result);
                ConsoleUtils.success("评论已发布");
            }
            
            return 0;
        } catch (IOException e) {
            ConsoleUtils.error("访问 GitHub 失败: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            if (e instanceof InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                ConsoleUtils.error("操作已中断");
                return 1;
            }
            ConsoleUtils.error("错误: " + e.getMessage());
            return 1;
        }
    }
}
