package com.github.ai.assistant.cli;

import com.github.ai.assistant.mcp.McpToolsProvider;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(
    name = "mcp-server",
    description = "启动 MCP Server（STDIO 模式），供 Claude Desktop / Cursor 等客户端调用"
)
public class McpServerCommand implements Runnable {

    private final McpToolsProvider mcpToolsProvider;

    public McpServerCommand(McpToolsProvider mcpToolsProvider) {
        this.mcpToolsProvider = mcpToolsProvider;
    }

    @Override
    public void run() {
        McpSyncServer server = mcpToolsProvider.createServer();
        System.err.println("🚀 GitHub AI Assistant MCP Server 已启动 (STDIO 模式)");
        System.err.println("   可用工具: " + String.join(", ", mcpToolsProvider.toolNames()));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("\n⏹ MCP Server 已停止");
            server.close();
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
