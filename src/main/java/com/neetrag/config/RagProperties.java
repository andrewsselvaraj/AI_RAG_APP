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

    public record Claude(String model, long maxTokens, String effort) {
    }
}
