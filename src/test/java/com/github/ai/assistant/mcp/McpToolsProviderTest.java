package com.github.ai.assistant.mcp;

import com.github.ai.assistant.config.AppConfig;
import com.github.ai.assistant.service.AIService;
import com.github.ai.assistant.service.ExplainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@DisplayName("McpToolsProvider 测试")
class McpToolsProviderTest {

    @Test
    @DisplayName("应暴露稳定的工具列表和服务版本")
    void shouldExposeToolNamesAndVersion() {
        AppConfig appConfig = new AppConfig();
        appConfig.setVersion("0.2.0-test");

        McpToolsProvider provider = new McpToolsProvider(
            appConfig,
            mock(AIService.class),
            mock(ExplainService.class)
        );

        assertEquals("0.2.0-test", provider.serverVersion());
        assertEquals(
            List.of(
                "generate_commit_message",
                "generate_gitignore",
                "explain_code",
                "review_code",
                "generate_readme"
            ),
            provider.toolNames()
        );
    }
}
