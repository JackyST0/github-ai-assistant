package com.github.ai.assistant.mcp;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.service.AIService;
import com.github.ai.assistant.service.ExplainService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class McpToolsProvider {

    private static final List<String> TOOL_NAMES = List.of(
        "generate_commit_message",
        "generate_gitignore",
        "explain_code",
        "review_code",
        "generate_readme"
    );

    private final AppConfig appConfig;
    private final AIService aiService;
    private final ExplainService explainService;

    public McpToolsProvider(AppConfig appConfig, AIService aiService, ExplainService explainService) {
        this.appConfig = appConfig;
        this.aiService = aiService;
        this.explainService = explainService;
    }

    public McpSyncServer createServer() {
        return McpServer.sync(new StdioServerTransportProvider())
            .serverInfo("github-ai-assistant", serverVersion())
            .tools(commitTool(), ignoreTool(), explainTool(), reviewTool(), readmeTool())
            .build();
    }

    public List<String> toolNames() {
        return TOOL_NAMES;
    }

    public String serverVersion() {
        return appConfig.getVersion();
    }

    private SyncToolSpecification commitTool() {
        var schema = new JsonSchema("object",
            Map.of(
                "diff", Map.of("type", "string", "description", "Git diff 内容"),
                "language", Map.of("type", "string", "description", "语言: zh 或 en，默认使用应用配置"),
                "format", Map.of("type", "string", "description", "格式: conventional 或 simple，默认 conventional")
            ),
            List.of("diff"), false);

        var tool = new Tool("generate_commit_message",
            "根据 Git diff 内容生成规范的 commit message，支持 Conventional Commits 格式", schema);

        return new SyncToolSpecification(tool, (exchange, args) -> {
            String diff = str(args, "diff");
            String lang = aiService.resolveLanguage(str(args, "language", appConfig.getAi().getDefaultLanguage()));
            String fmt = str(args, "format", "conventional");

            String prompt = buildCommitPrompt(lang, fmt);
            String msg = "请根据以下 Git diff 生成 commit message：\n\n```diff\n%s\n```\n\n只输出 commit message，不要其他解释。"
                .formatted(truncate(diff, 4000));

            return textResult(aiService.chat(null, prompt, msg));
        });
    }

    private SyncToolSpecification ignoreTool() {
        var schema = new JsonSchema("object",
            Map.of(
                "projectTypes", Map.of("type", "string", "description", "项目类型，如: Java, Node.js, Python"),
                "buildTools", Map.of("type", "string", "description", "构建工具，如: Maven, Gradle, npm"),
                "ides", Map.of("type", "string", "description", "IDE，如: IntelliJ IDEA, VS Code"),
                "frameworks", Map.of("type", "string", "description", "框架，如: Spring Boot, React")
            ),
            List.of("projectTypes", "buildTools"), false);

        var tool = new Tool("generate_gitignore",
            "根据项目类型、构建工具等信息，智能生成 .gitignore 文件内容", schema);

        return new SyncToolSpecification(tool, (exchange, args) -> {
            String sys = "你是一个专业的软件工程师，擅长创建完善的 .gitignore 文件。按类别分组，使用中文注释。只输出 .gitignore 内容。";
            StringBuilder sb = new StringBuilder("请根据以下项目信息生成 .gitignore：\n\n");
            sb.append("项目类型: ").append(str(args, "projectTypes")).append("\n");
            sb.append("构建工具: ").append(str(args, "buildTools")).append("\n");
            if (args.containsKey("ides")) sb.append("IDE: ").append(str(args, "ides")).append("\n");
            if (args.containsKey("frameworks")) sb.append("框架: ").append(str(args, "frameworks")).append("\n");
            return textResult(aiService.chat(null, sys, sb.toString()));
        });
    }

    private SyncToolSpecification explainTool() {
        var schema = new JsonSchema("object",
            Map.of(
                "content", Map.of("type", "string", "description", "要解释的代码或命令"),
                "type", Map.of("type", "string", "description", "类型: git-command 或 code，默认 code"),
                "language", Map.of("type", "string", "description", "输出语言: zh 或 en，默认使用应用配置")
            ),
            List.of("content"), false);

        var tool = new Tool("explain_code", "解释代码片段或 Git 命令的含义和用法", schema);

        return new SyncToolSpecification(tool, (exchange, args) -> {
            String result = explainService.explain(
                str(args, "content"), str(args, "type", "code"),
                aiService.resolveLanguage(str(args, "language", appConfig.getAi().getDefaultLanguage())),
                "detailed",
                null);
            return textResult(result);
        });
    }

    private SyncToolSpecification reviewTool() {
        var schema = new JsonSchema("object",
            Map.of(
                "code", Map.of("type", "string", "description", "要审查的代码或 diff"),
                "focus", Map.of("type", "string", "description", "审查重点: security, performance, style, all，默认 all")
            ),
            List.of("code"), false);

        var tool = new Tool("review_code", "AI 代码审查，分析问题并提供改进建议", schema);

        return new SyncToolSpecification(tool, (exchange, args) -> {
            String focus = str(args, "focus", "all");
            String focusDesc = switch (focus.toLowerCase()) {
                case "security" -> "重点关注安全问题";
                case "performance" -> "重点关注性能问题";
                case "style" -> "重点关注代码风格";
                default -> "全面审查：安全、性能、代码风格、最佳实践";
            };
            String sys = "你是资深代码审查专家。审查重点：%s\n输出格式：## 总结 ## 评分(0-100) ## 问题 ## 建议".formatted(focusDesc);
            String msg = "请审查以下代码：\n\n```\n%s\n```".formatted(truncate(str(args, "code"), 6000));
            return textResult(aiService.chat(null, sys, msg));
        });
    }

    private SyncToolSpecification readmeTool() {
        var schema = new JsonSchema("object",
            Map.of(
                "projectName", Map.of("type", "string", "description", "项目名称"),
                "description", Map.of("type", "string", "description", "项目描述"),
                "projectTypes", Map.of("type", "string", "description", "项目类型，如: Java/Maven"),
                "language", Map.of("type", "string", "description", "输出语言: zh 或 en，默认使用应用配置")
            ),
            List.of("projectName", "description", "projectTypes"), false);

        var tool = new Tool("generate_readme", "根据项目信息智能生成 README.md 内容", schema);

        return new SyncToolSpecification(tool, (exchange, args) -> {
            String lang = aiService.resolveLanguage(str(args, "language", appConfig.getAi().getDefaultLanguage()));
            String sys = lang.equals("zh")
                ? "你是技术文档专家。生成包含项目标题、功能、技术栈、快速开始、用法的 README.md。中文 Markdown。只输出内容。"
                : "Tech docs expert. Generate README.md with title, features, tech stack, quick start, usage. English Markdown. Content only.";
            String msg = "项目名称: %s\n项目类型: %s\n项目描述: %s".formatted(
                str(args, "projectName"), str(args, "projectTypes"), str(args, "description"));
            return textResult(aiService.chat(null, sys, msg));
        });
    }

    private static CallToolResult textResult(String text) {
        return new CallToolResult(List.of(new TextContent(text)), false);
    }

    private static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v != null ? v.toString() : "";
    }

    private static String str(Map<String, Object> args, String key, String defaultVal) {
        Object v = args.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : defaultVal;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "\n... (truncated)";
    }

    private static String buildCommitPrompt(String language, String format) {
        boolean zh = language.equals("zh");
        String fmtInst = format.equals("conventional")
            ? "使用 Conventional Commits 格式：<type>(<scope>): <subject>"
            : (zh ? "生成简洁一行 commit message。" : "Generate a concise one-line commit message.");
        if (zh) {
            return "你是专业软件工程师，擅长写 Git commit message。%s\ntype/scope 英文，subject/body 中文，subject 不超 50 字。".formatted(fmtInst);
        }
        return "Professional engineer writing Git commit messages. %s\nSubject under 50 chars, imperative mood.".formatted(fmtInst);
    }
}
