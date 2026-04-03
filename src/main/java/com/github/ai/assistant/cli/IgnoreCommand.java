package com.github.ai.assistant.cli;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.service.IgnoreService;
import com.github.ai.assistant.service.IgnoreService.ProjectInfo;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * 智能 .gitignore 生成命令
 * 
 * 分析项目结构，自动生成合适的 .gitignore 文件
 */
@Component
@Command(
    name = "ignore",
    description = "智能生成 .gitignore 文件",
    mixinStandardHelpOptions = true
)
public class IgnoreCommand implements Callable<Integer> {

    private final IgnoreService ignoreService;

    @Option(names = {"-d", "--dir"}, description = "项目目录 (默认为当前目录)")
    private File directory = new File(".");

    @Option(names = {"-o", "--output"}, description = "输出文件名 (默认为 .gitignore)")
    private String outputFile = ".gitignore";

    @Option(names = {"--append"}, description = "追加到现有 .gitignore 而非覆盖")
    private boolean append;

    @Option(names = {"--dry-run"}, description = "仅预览不写入文件")
    private boolean dryRun;

    @Option(names = {"-y", "--yes"}, description = "跳过确认直接写入")
    private boolean autoConfirm;

    @Option(names = {"-m", "--model"}, description = "AI 模型 (openai/ollama)，默认使用应用配置")
    private String model;

    public IgnoreCommand(IgnoreService ignoreService, AppConfig appConfig) {
        this.ignoreService = ignoreService;
        this.model = appConfig.getAi().getDefaultModel();
    }

    @Override
    public Integer call() {
        try {
            Path projectPath = directory.toPath().toAbsolutePath();
            
            if (!Files.isDirectory(projectPath)) {
                ConsoleUtils.error("目录不存在: " + projectPath);
                return 1;
            }

            // 分析项目
            ConsoleUtils.section("🔍 分析项目结构...");
            ProjectInfo projectInfo = ignoreService.analyzeProject(projectPath);
            
            // 显示检测结果
            ConsoleUtils.section("📋 检测结果：");
            ConsoleUtils.detail("项目类型", String.join(", ", projectInfo.projectTypes()));
            ConsoleUtils.detail("构建工具", String.join(", ", projectInfo.buildTools()));
            ConsoleUtils.detail("IDE/编辑器", String.join(", ", projectInfo.ides()));
            if (!projectInfo.frameworks().isEmpty()) {
                ConsoleUtils.detail("框架", String.join(", ", projectInfo.frameworks()));
            }
            ConsoleUtils.separator();

            // 读取现有 .gitignore（用于追加模式参考）
            Path gitignorePath = projectPath.resolve(outputFile);
            final String existingContent = Files.exists(gitignorePath) 
                ? Files.readString(gitignorePath) 
                : "";

            // 生成 .gitignore
            String gitignoreContent = ConsoleUtils.withSpinner("🤖 AI 正在生成 .gitignore...",
                () -> ignoreService.generateGitignore(projectInfo, existingContent, append, model));

            // 显示生成内容
            ConsoleUtils.section("📄 生成的 .gitignore：");
            ConsoleUtils.line(gitignoreContent);
            ConsoleUtils.separator();

            // Dry-run 模式
            if (dryRun) {
                ConsoleUtils.blankLine();
                ConsoleUtils.line("✨ [Dry Run] 未写入文件");
                ConsoleUtils.line("💡 提示：移除 --dry-run 参数可写入文件");
                return 0;
            }

            // 检查是否存在现有文件，询问是否覆盖
            boolean fileExists = Files.exists(gitignorePath);
            if (fileExists && !append && !autoConfirm) {
                ConsoleUtils.warn("检测到已存在 " + outputFile + " 文件");
                boolean shouldOverwrite = ConsoleUtils.confirm("是否覆盖现有文件?", false);
                if (!shouldOverwrite) {
                    // 询问是否改为追加模式
                    boolean useAppend = ConsoleUtils.confirm("是否改为追加模式（保留原有内容）?", true);
                    if (useAppend) {
                        String finalContent = existingContent + "\n\n" + "# === AI 生成的内容 ===\n" + gitignoreContent;
                        Files.writeString(gitignorePath, finalContent);
                        ConsoleUtils.success(outputFile + " 已追加！");
                        return 0;
                    } else {
                        ConsoleUtils.info("已取消");
                        return 0;
                    }
                }
            }

            // 确认写入
            boolean shouldWrite = autoConfirm || ConsoleUtils.confirm("\n是否写入到 " + outputFile + "?", true);

            if (shouldWrite) {
                if (append && !existingContent.isEmpty()) {
                    // 追加模式
                    String finalContent = existingContent + "\n\n" + "# === AI 生成的内容 ===\n" + gitignoreContent;
                    Files.writeString(gitignorePath, finalContent);
                } else {
                    Files.writeString(gitignorePath, gitignoreContent);
                }
                ConsoleUtils.success(outputFile + " 已生成！");
            } else {
                ConsoleUtils.info("已取消");
            }

            return 0;
        } catch (IOException e) {
            ConsoleUtils.error("I/O 错误: " + e.getMessage());
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
