package com.github.ai.assistant.client;

import com.github.ai.assistant.config.AppConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GitHubClientService 测试")
class GitHubClientServiceTest {

    @Test
    @DisplayName("初始化时应使用配置中的 GitHub API 地址")
    void shouldInitializeGitHubClientWithConfiguredApiUrl() throws Exception {
        AppConfig appConfig = new AppConfig();
        appConfig.getGithub().setToken("test-token");
        appConfig.getGithub().setApiUrl("https://ghe.example.com/api/v3");

        GitHubBuilder builder = mock(GitHubBuilder.class);
        GitHub github = mock(GitHub.class);
        when(builder.withEndpoint(anyString())).thenReturn(builder);
        when(builder.withOAuthToken(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(github);

        new GitHubClientService(appConfig, () -> builder);

        verify(builder).withEndpoint("https://ghe.example.com/api/v3");
        verify(builder).withOAuthToken("test-token");
        verify(builder).build();
    }

    @Test
    @DisplayName("未配置 token 时应在首次使用时抛出清晰异常")
    void shouldFailFastWhenTokenIsMissing() {
        AppConfig appConfig = new AppConfig();
        appConfig.getGithub().setToken("");

        GitHubClientService service = new GitHubClientService(appConfig, GitHubBuilder::new);

        IllegalStateException exception = assertThrows(IllegalStateException.class, service::getCurrentUser);
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("GitHub Token 未配置"));
    }

    @Test
    @DisplayName("连接检查在 GitHub API 抛出异常时应返回 false")
    void shouldReturnFalseWhenConnectionCheckFails() throws Exception {
        AppConfig appConfig = new AppConfig();
        appConfig.getGithub().setToken("test-token");

        GitHubBuilder builder = mock(GitHubBuilder.class);
        GitHub github = mock(GitHub.class);
        when(builder.withEndpoint(anyString())).thenReturn(builder);
        when(builder.withOAuthToken(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(github);
        when(github.getMyself()).thenThrow(new IOException("boom"));

        GitHubClientService service = new GitHubClientService(appConfig, () -> builder);

        org.junit.jupiter.api.Assertions.assertFalse(service.isConnected());
    }
}
