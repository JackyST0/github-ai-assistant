package com.github.ai.assistant.cli;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.service.IssueService;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Issue 管理命令
 * 
 * 使用 AI 进行 Issue 分类、回复建议等
 */
@Component
@Command(
    name = "issue",
    description = "AI 智能 Issue 管理",
    mixinStandardHelpOptions = true
)
public class IssueCommand implements Callable<Integer> {

    private final IssueService issueService;

    @Option(names = {"--id"}, description = "Issue 编号")
    private Integer issueNumber;

    @Option(names = {"-r", "--repo"}, description = "仓库名 (格式: owner/repo)")
    private String repository;

    @Option(names = {"--action"}, description = "操作类型 (classify/suggest/summarize)", defaultValue = "suggest")
    private String action;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)，默认使用应用配置")
    private String model;

    public IssueCommand(IssueService issueService, AppConfig appConfig) {
        this.issueService = issueService;
        this.model = appConfig.getAi().getDefaultModel();
    }

    @Override
    public Integer call() {
        try {
            return switch (action) {
                case "classify" -> classifyIssue();
                case "suggest" -> suggestReply();
                case "summarize" -> summarizeIssues();
                default -> {
                    ConsoleUtils.error("未知操作: " + action);
                    yield 1;
                }
            };
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

    private Integer classifyIssue() throws Exception {
        if (issueNumber == null) {
            ConsoleUtils.error("请使用 --id 指定 Issue 编号");
            return 1;
        }
        
        var classification = ConsoleUtils.withSpinner("🏷️ 正在分析 Issue #" + issueNumber + "...",
            () -> issueService.classifyIssue(repository, issueNumber, model));
        
        ConsoleUtils.section("📋 Issue 分类结果");
        ConsoleUtils.detail("类型", classification.type());
        ConsoleUtils.detail("优先级", classification.priority());
        ConsoleUtils.detail("建议标签", String.join(", ", classification.suggestedLabels()));
        ConsoleUtils.separator();
        
        return 0;
    }

    private Integer suggestReply() throws Exception {
        if (issueNumber == null) {
            ConsoleUtils.error("请使用 --id 指定 Issue 编号");
            return 1;
        }
        
        String suggestion = ConsoleUtils.withSpinner("💬 正在生成回复建议...",
            () -> issueService.suggestReply(repository, issueNumber, model));
        
        ConsoleUtils.section("📝 建议回复");
        ConsoleUtils.line(suggestion);
        ConsoleUtils.separator();
        
        return 0;
    }

    private Integer summarizeIssues() throws Exception {
        String summary = ConsoleUtils.withSpinner("📊 正在汇总 Issues...",
            () -> issueService.summarizeOpenIssues(repository, model));
        
        ConsoleUtils.doubleSection("📋 Issues 汇总");
        ConsoleUtils.line(summary);
        ConsoleUtils.doubleSeparator();
        
        return 0;
    }
}
