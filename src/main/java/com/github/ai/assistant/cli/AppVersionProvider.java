package com.github.ai.assistant.cli;

import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppVersionProvider implements IVersionProvider {

    private static final String VERSION_KEY = "app.version";

    @Override
    public String[] getVersion() {
        Properties properties = new Properties();
        try (InputStream input = AppVersionProvider.class.getClassLoader().getResourceAsStream("build.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            // Fall back to the default version string when build metadata is unavailable.
        }

        String version = properties.getProperty(VERSION_KEY, "dev");
        return new String[] { "GitHub AI Assistant " + version };
    }
}
