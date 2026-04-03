package com.github.ai.assistant.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ReadmeService 测试")
class ReadmeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("analyzeProject 应提取 Maven 项目信息与结构")
    void shouldAnalyzeMavenProject() throws Exception {
        Files.writeString(tempDir.resolve("pom.xml"), """
            <project>
              <artifactId>demo-app</artifactId>
              <description>Sample project</description>
            </project>
            """);
        Files.createDirectories(tempDir.resolve("src/main/java/com/example"));
        Files.writeString(tempDir.resolve("src/main/java/com/example/App.java"), "class App {}");

        ReadmeService service = new ReadmeService(mock(AIService.class));

        ReadmeService.ProjectContext context = service.analyzeProject(tempDir);

        assertEquals("demo-app", context.projectName());
        assertEquals("Sample project", context.description());
        assertTrue(context.projectTypes().contains("Java/Maven"));
        assertTrue(context.mainFiles().contains("pom.xml"));
        assertTrue(context.structureTree().contains("src/"));
    }

    @Test
    @DisplayName("generateReadme 应将上下文交给 AIService")
    void shouldDelegateReadmeGenerationToAiService() {
        AIService aiService = mock(AIService.class);
        when(aiService.resolveLanguage(anyString())).thenReturn("en");
        when(aiService.chat(anyString(), anyString(), anyString())).thenReturn("# Demo");

        ReadmeService service = new ReadmeService(aiService);
        ReadmeService.ProjectContext context = new ReadmeService.ProjectContext(
            "demo-app",
            "Sample project",
            java.util.List.of("Java/Maven"),
            java.util.List.of("Spring Boot"),
            java.util.List.of("pom.xml"),
            "demo-app/\n└── pom.xml\n"
        );

        String result = service.generateReadme(context, "en", "ollama");

        assertEquals("# Demo", result);
        verify(aiService).chat(anyString(), anyString(), anyString());
    }
}
