package com.github.ai.assistant.cli;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.util.ConsoleUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * 主命令入口
 * 
 * 使用方式：
 *   gh-ai commit     - 生成 commit message
 *   gh-ai review     - PR 代码审查
 *   gh-ai explain    - 解释代码或命令
 *   gh-ai issue      - Issue 管理
 *   gh-ai ignore     - 智能生成 .gitignore
 *   gh-ai readme     - 智能生成 README.md
 */
@Component
@Command(
    name = "gh-ai",
    description = "AI-powered GitHub Assistant - 智能 GitHub 助手",
    mixinStandardHelpOptions = true,
    versionProvider = AppVersionProvider.class,
    subcommands = {
        CommitCommand.class,
        ReviewCommand.class,
        ExplainCommand.class,
        IssueCommand.class,
        IgnoreCommand.class,
        ReadmeCommand.class,
        McpServerCommand.class
    }
)
public class MainCommand implements Runnable {

    private final AppConfig appConfig;
    private final String openAiApiKey;
    private final String openAiBaseUrl;
    private final String ollamaBaseUrl;

    @Option(names = {"-v", "--verbose"}, description = "显示详细输出")
    private boolean verbose;

    public MainCommand(
            AppConfig appConfig,
            @Value("${spring.ai.openai.api-key:}") String openAiApiKey,
            @Value("${spring.ai.openai.base-url:https://api.openai.com}") String openAiBaseUrl,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
        this.appConfig = appConfig;
        this.openAiApiKey = openAiApiKey;
        this.openAiBaseUrl = openAiBaseUrl;
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    @Override
    public void run() {
        boolean hasOpenAiKey = hasText(openAiApiKey);
        boolean hasGithubToken = hasText(appConfig.getGithub().getToken());
        String defaultModel = appConfig.getAi().getDefaultModel();
        String defaultLanguage = appConfig.getAi().getDefaultLanguage();
        boolean supportedDefaultModel = isSupportedModel(defaultModel);
        boolean defaultModelReady = isDefaultModelReady(defaultModel, hasOpenAiKey);
        String version = appConfig.getVersion();

        ConsoleUtils.blankLine();
        ConsoleUtils.doubleSeparator();
        ConsoleUtils.line("🤖 GitHub AI Assistant v" + version);
        ConsoleUtils.doubleSeparator();
        ConsoleUtils.blankLine();
        ConsoleUtils.line("AI 驱动的 GitHub 智能助手");
        ConsoleUtils.blankLine();

        ConsoleUtils.line("📋 配置状态：");
        ConsoleUtils.detail("默认模型", defaultModel + (defaultModelReady ? " ✅" : " ❌"));
        ConsoleUtils.detail("默认语言", defaultLanguage);
        ConsoleUtils.detail("OpenAI API Key", hasOpenAiKey ? "✅ 已配置" : "❌ 未配置");
        ConsoleUtils.detail("GitHub Token", hasGithubToken ? "✅ 已配置" : "⚠️  未配置 (PR/Issue 功能需要)");
        
        if (verbose) {
            printVerboseConfig();
        }

        if (!supportedDefaultModel) {
            ConsoleUtils.blankLine();
            ConsoleUtils.warn("当前默认模型不受支持，请使用 openai 或 ollama。");
        }

        if (!defaultModelReady) {
            ConsoleUtils.line("""
            ⚠️  当前默认模型不可用，请配置 AI 服务：
            
               方式一：环境变量
               export OPENAI_API_KEY=your_api_key
               export OPENAI_BASE_URL=https://api.openai.com  # 可选
               export OPENAI_MODEL=gpt-4o-mini                # 可选

               方式二：application.yml
               spring.ai.openai.api-key=your_api_key
            """);
        }

        ConsoleUtils.blankLine();
        ConsoleUtils.line("📖 可用命令：");
        ConsoleUtils.line("   commit   - 根据代码变更生成 commit message");
        ConsoleUtils.line("   review   - AI 审查 Pull Request");
        ConsoleUtils.line("   explain  - 解释代码或 Git 命令");
        ConsoleUtils.line("   issue    - Issue 智能管理");
        ConsoleUtils.line("   ignore   - 智能生成 .gitignore 文件");
        ConsoleUtils.line("   readme   - 智能生成 README.md 文件");
        ConsoleUtils.line("   mcp-server - 启动 MCP Server（STDIO）");
        ConsoleUtils.blankLine();
        ConsoleUtils.line("🚀 快速开始：");
        ConsoleUtils.line("   gh-ai explain \"git rebase -i\"   # 解释 git 命令");
        ConsoleUtils.line("   gh-ai commit                    # 生成 commit message");
        ConsoleUtils.line("   gh-ai ignore                    # 生成 .gitignore");
        ConsoleUtils.line("   gh-ai readme                    # 生成 README.md");
        ConsoleUtils.line("   gh-ai review --repo owner/repo --pr 123");
        ConsoleUtils.line("   gh-ai mcp-server");
        ConsoleUtils.blankLine();
        ConsoleUtils.line("💡 使用 'gh-ai <command> --help' 查看详细帮助");
        ConsoleUtils.doubleSeparator();
    }

    private void printVerboseConfig() {
        ConsoleUtils.blankLine();
        ConsoleUtils.line("🔎 详细配置：");
        ConsoleUtils.detail("OpenAI Base URL", openAiBaseUrl);
        ConsoleUtils.detail("Ollama Base URL", ollamaBaseUrl);
        ConsoleUtils.detail("GitHub API URL", appConfig.getGithub().getApiUrl());
    }

    private boolean isDefaultModelReady(String defaultModel, boolean hasOpenAiKey) {
        String normalizedModel = defaultModel == null ? "" : defaultModel.trim().toLowerCase();
        return switch (normalizedModel) {
            case "ollama", "local" -> true;
            case "openai", "gpt", "" -> hasOpenAiKey;
            default -> false;
        };
    }

    private boolean isSupportedModel(String defaultModel) {
        String normalizedModel = defaultModel == null ? "" : defaultModel.trim().toLowerCase();
        return normalizedModel.isEmpty()
            || normalizedModel.equals("openai")
            || normalizedModel.equals("gpt")
            || normalizedModel.equals("ollama")
            || normalizedModel.equals("local");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
