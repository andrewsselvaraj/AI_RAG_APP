package com.neetrag.config;

import java.io.File;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel, RagProperties props) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File file = new File(props.vectorStoreFile());
        if (file.exists()) {
            log.info("Loading vector store from {}", file.getAbsolutePath());
            store.load(file);
        }
        return store;
    }

    /** Reads ANTHROPIC_API_KEY from the environment. */
    @Bean(destroyMethod = "close")
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
