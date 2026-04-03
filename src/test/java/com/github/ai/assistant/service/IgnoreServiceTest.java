package com.github.ai.assistant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IgnoreService 测试")
class IgnoreServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("analyzeProject 应识别 Java Maven Spring Boot 项目")
    void shouldDetectJavaMavenSpringBootProject() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), "<project />");
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.writeString(
            tempDir.resolve("src/main/java/com/example/Application.java"),
            "@SpringBootApplication class Application {}"
        );
        Files.createDirectories(tempDir.resolve(".idea"));

        IgnoreService service = new IgnoreService(mock(AIService.class));

        IgnoreService.ProjectInfo projectInfo = service.analyzeProject(tempDir);

        assertTrue(projectInfo.projectTypes().contains("Java"));
        assertTrue(projectInfo.buildTools().contains("Maven"));
        assertTrue(projectInfo.frameworks().contains("Spring Boot"));
        assertTrue(projectInfo.ides().contains("IntelliJ IDEA"));
    }

    @Test
    @DisplayName("generateGitignore 在追加模式下应把现有内容作为上下文传给 AI")
    void shouldIncludeExistingContentWhenAppendModeIsEnabled() {
        AIService aiService = mock(AIService.class);
        when(aiService.chat(anyString(), anyString(), anyString())).thenReturn("target/");

        IgnoreService service = new IgnoreService(aiService);
        IgnoreService.ProjectInfo projectInfo = new IgnoreService.ProjectInfo(
            java.util.List.of("Java"),
            java.util.List.of("Maven"),
            java.util.List.of("IntelliJ IDEA"),
            java.util.List.of("Spring Boot"),
            java.util.List.of("pom.xml")
        );

        service.generateGitignore(projectInfo, ".idea/\n", true, "openai");

        verify(aiService).chat(anyString(), anyString(), org.mockito.ArgumentMatchers.contains("现有 .gitignore 内容"));
    }
}
