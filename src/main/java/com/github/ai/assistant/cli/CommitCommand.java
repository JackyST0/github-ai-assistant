package com.github.ai.assistant.cli;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.service.CommitService;
import com.github.ai.assistant.service.CommitService.CommitResult;
import com.github.ai.assistant.service.CommitService.DiffStats;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Commit Message 生成命令
 * 
 * 根据当前的 staged changes 或指定的 diff 生成 commit message
 */
@Component
@Command(
    name = "commit",
    description = "根据代码变更智能生成 commit message",
    mixinStandardHelpOptions = true
)
public class CommitCommand implements Callable<Integer> {

    private final CommitService commitService;

    @Option(names = {"-d", "--dir"}, description = "Git 仓库目录 (默认为当前目录)")
    private File directory = new File(".");

    @Option(names = {"-l", "--lang"}, description = "Commit message 语言 (zh/en)，默认使用应用配置")
    private String language;

    @Option(names = {"-t", "--type"}, description = "Commit 类型 (conventional/simple)", defaultValue = "conventional")
    private String type;

    @Option(names = {"--dry-run"}, description = "仅生成不执行 commit")
    private boolean dryRun;

    @Option(names = {"-y", "--yes"}, description = "跳过确认直接执行 commit")
    private boolean autoConfirm;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)，默认使用应用配置")
    private String model;

    @Option(names = {"--show-files"}, description = "显示将要提交的文件列表")
    private boolean showFiles;

    public CommitCommand(CommitService commitService, AppConfig appConfig) {
        this.commitService = commitService;
        this.language = appConfig.getAi().getDefaultLanguage();
        this.model = appConfig.getAi().getDefaultModel();
    }

    @Override
    public Integer call() {
        try {
            // 获取 staged 文件信息
            List<String> stagedFiles = commitService.getStagedFiles(directory.toPath());
            
            if (stagedFiles.isEmpty()) {
                ConsoleUtils.warn("没有 staged 的文件。请先使用 git add 添加文件。");
                return 1;
            }
            
            // 显示变更统计
            DiffStats stats = commitService.getStagedStats(directory.toPath());
            ConsoleUtils.section("📊 变更统计");
            ConsoleUtils.line(String.format("   文件数: %d | 新增: +%d | 删除: -%d",
                stats.filesChanged(), stats.insertions(), stats.deletions()));
            
            // 可选：显示文件列表
            if (showFiles) {
                ConsoleUtils.blankLine();
                ConsoleUtils.line("📁 Staged 文件:");
                stagedFiles.forEach(ConsoleUtils::bullet);
            }
            
            ConsoleUtils.separator();
            
            // 生成 commit message
            String commitMessage = ConsoleUtils.withSpinner("🤖 AI 正在分析代码变更...",
                () -> commitService.generateCommitMessage(directory.toPath(), language, type, model));
            
            // 显示生成的 message
            ConsoleUtils.line("📝 生成的 Commit Message：");
            ConsoleUtils.separator();
            ConsoleUtils.line(commitMessage);
            ConsoleUtils.separator();
            
            // Dry-run 模式
            if (dryRun) {
                ConsoleUtils.blankLine();
                ConsoleUtils.line("✨ [Dry Run] 未执行实际 commit");
                ConsoleUtils.line("💡 提示：移除 --dry-run 参数可执行实际提交");
                return 0;
            }
            
            // 确认并执行 commit
            boolean shouldCommit = autoConfirm || ConsoleUtils.confirm("\n是否使用此 message 进行 commit?", true);
            
            if (shouldCommit) {
                ConsoleUtils.blankLine();
                ConsoleUtils.line("⏳ 正在执行 git commit...");
                
                CommitResult result = commitService.executeCommit(directory.toPath(), commitMessage);
                
                if (result.success()) {
                    ConsoleUtils.success("Commit 成功！");
                    ConsoleUtils.detail("Commit Hash", result.commitHash());
                } else {
                    ConsoleUtils.error("Commit 失败");
                    ConsoleUtils.line("   " + result.message());
                    return 1;
                }
            } else {
                ConsoleUtils.info("已取消 commit");
                ConsoleUtils.blankLine();
                ConsoleUtils.line("💡 提示：");
                ConsoleUtils.bullet("使用 -y 参数可跳过确认直接提交");
                ConsoleUtils.bullet("使用 --dry-run 参数可仅生成不执行");
            }
            
            return 0;
        } catch (IllegalStateException e) {
            ConsoleUtils.warn(e.getMessage());
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ConsoleUtils.error("操作已中断");
            return 1;
        } catch (IOException e) {
            ConsoleUtils.error("I/O 错误: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            ConsoleUtils.error("错误: " + e.getMessage());
            return 1;
        }
    }
}
