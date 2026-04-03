package com.github.ai.assistant;

import com.github.ai.assistant.cli.MainCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.ai.autoconfigure.ollama.OllamaAutoConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication(exclude = {
    OpenAiAutoConfiguration.class,
    OllamaAutoConfiguration.class
})
public class GithubAiAssistantApplication implements CommandLineRunner, ExitCodeGenerator {

    private final IFactory factory;
    private final MainCommand mainCommand;
    private int exitCode;

    public GithubAiAssistantApplication(IFactory factory, MainCommand mainCommand) {
        this.factory = factory;
        this.mainCommand = mainCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(GithubAiAssistantApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(mainCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
