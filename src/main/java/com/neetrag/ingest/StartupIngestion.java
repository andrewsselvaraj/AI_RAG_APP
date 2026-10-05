package com.neetrag.ingest;

import java.io.File;

import com.neetrag.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Builds the vector store on first start, when no saved store file exists yet. */
@Component
public class StartupIngestion implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupIngestion.class);

    private final IngestionService ingestionService;
    private final RagProperties props;

    public StartupIngestion(IngestionService ingestionService, RagProperties props) {
        this.ingestionService = ingestionService;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (props.ingestOnStartup() && !new File(props.vectorStoreFile()).exists()) {
            log.info("No vector store found, ingesting data...");
            log.info("Ingestion done: {}", ingestionService.ingestAll());
        }
    }
}
