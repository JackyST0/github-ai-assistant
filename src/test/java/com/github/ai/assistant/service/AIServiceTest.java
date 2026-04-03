package com.github.ai.assistant.service;

import com.github.ai.assistant.config.AppConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@DisplayName("AIService 测试")
class AIServiceTest {

    @Test
    @DisplayName("应在模型或语言缺失时回退到应用默认配置")
    void shouldResolveDefaultsFromAppConfig() {
        AppConfig appConfig = new AppConfig();
        appConfig.getAi().setDefaultModel("ollama");
        appConfig.getAi().setDefaultLanguage("en");

        AIService aiService = new AIService(
            mock(ChatModel.class),
            mock(ChatModel.class),
            appConfig
        );

        assertEquals("ollama", aiService.resolveModel(null));
        assertEquals("ollama", aiService.resolveModel("   "));
        assertEquals("openai", aiService.resolveModel("openai"));
        assertEquals("ollama", aiService.resolveModel("local"));
        assertEquals("openai", aiService.resolveModel("gpt"));
        assertThrows(IllegalArgumentException.class, () -> aiService.resolveModel("bedrock"));

        assertEquals("en", aiService.resolveLanguage(null));
        assertEquals("en", aiService.resolveLanguage(" "));
        assertEquals("zh", aiService.resolveLanguage("zh"));
    }
}
