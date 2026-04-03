package com.github.ai.assistant.cli;

import com.github.ai.assistant.mcp.McpToolsProvider;
import io.modelcontextprotocol.server.McpSyncServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("McpServerCommand 测试")
class McpServerCommandTest {

    private PrintStream originalErr;
    private ByteArrayOutputStream errorOutput;

    @BeforeEach
    void setUp() {
        originalErr = System.err;
        errorOutput = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorOutput, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setErr(originalErr);
    }

    @Test
    @Timeout(5)
    @DisplayName("启动时应输出工具列表并在中断后退出")
    void shouldPrintStartupMessageAndExitOnInterrupt() throws Exception {
        McpToolsProvider provider = mock(McpToolsProvider.class);
        McpSyncServer server = mock(McpSyncServer.class);
        when(provider.createServer()).thenReturn(server);
        when(provider.toolNames()).thenReturn(List.of("generate_commit_message", "generate_readme"));

        McpServerCommand command = new McpServerCommand(provider);
        Thread thread = new Thread(command::run);

        thread.start();
        Thread.sleep(200);
        thread.interrupt();
        thread.join(2_000);

        assertFalse(thread.isAlive(), "命令线程应在收到中断后退出");

        String text = errorOutput.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("GitHub AI Assistant MCP Server 已启动"));
        assertTrue(text.contains("generate_commit_message, generate_readme"));
        verify(provider).createServer();
        verify(provider).toolNames();
    }
}
