package com.github.ai.assistant.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("AppVersionProvider 测试")
class AppVersionProviderTest {

    @Test
    @DisplayName("应从构建元数据读取版本号")
    void shouldReadVersionFromBuildProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = AppVersionProviderTest.class.getClassLoader().getResourceAsStream("build.properties")) {
            assertNotNull(input, "build.properties 应存在于测试类路径中");
            properties.load(input);
        }

        String expectedVersion = properties.getProperty("app.version");
        String[] version = new AppVersionProvider().getVersion();

        assertEquals("GitHub AI Assistant " + expectedVersion, version[0]);
    }
}
