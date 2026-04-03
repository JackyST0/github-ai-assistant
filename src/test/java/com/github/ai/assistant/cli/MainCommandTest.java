package com.github.ai.assistant.cli;

import com.github.ai.assistant.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MainCommand 测试")
class MainCommandTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    @DisplayName("应显示版本、默认配置和 MCP 命令")
    void shouldRenderVersionAndMcpCommand() {
        AppConfig appConfig = new AppConfig();
        appConfig.setVersion("9.9.9");
        appConfig.getAi().setDefaultModel("ollama");
        appConfig.getAi().setDefaultLanguage("en");
        appConfig.getGithub().setToken("ghp_test");

        MainCommand command = new MainCommand(
            appConfig,
            "",
            "https://api.openai.example.com",
            "http://localhost:11434"
        );

        command.run();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("GitHub AI Assistant v9.9.9"));
        assertTrue(text.contains("默认模型: ollama ✅"));
        assertTrue(text.contains("默认语言: en"));
        assertTrue(text.contains("mcp-server - 启动 MCP Server（STDIO）"));
        assertFalse(text.contains("当前默认模型不可用"));
    }

    @Test
    @DisplayName("verbose 模式应显示详细配置并提示缺失的 OpenAI Key")
    void shouldRenderVerboseConfigurationAndWarnings() throws Exception {
        AppConfig appConfig = new AppConfig();
        appConfig.setVersion("1.2.3");
        appConfig.getAi().setDefaultModel("openai");
        appConfig.getAi().setDefaultLanguage("zh");

        MainCommand command = new MainCommand(
            appConfig,
            "",
            "https://api.openai.example.com",
            "http://localhost:11434"
        );
        setVerbose(command, true);

        command.run();

        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("🔎 详细配置"));
        assertTrue(text.contains("OpenAI Base URL: https://api.openai.example.com"));
        assertTrue(text.contains("Ollama Base URL: http://localhost:11434"));
        assertTrue(text.contains("当前默认模型不可用"));
        assertTrue(text.contains("spring.ai.openai.api-key=your_api_key"));
    }

    private void setVerbose(MainCommand command, boolean value) throws Exception {
        Field verboseField = MainCommand.class.getDeclaredField("verbose");
        verboseField.setAccessible(true);
        verboseField.set(command, value);
    }
}
