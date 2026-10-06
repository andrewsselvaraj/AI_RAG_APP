package com.neetrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neet")
public record RagProperties(
        String questionsDir,
        String ncertDir,
        String vectorStoreFile,
        boolean ingestOnStartup,
        int topK,
        double similarityThreshold,
        Claude claude) {

    /** apiKey is optional: when blank, the ANTHROPIC_API_KEY environment variable is used. */
    public record Claude(String model, long maxTokens, String effort, String apiKey) {
    }
}
