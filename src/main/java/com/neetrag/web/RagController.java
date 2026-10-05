package com.neetrag.web;

import java.io.IOException;
import java.util.List;

import com.neetrag.ingest.IngestionService;
import com.neetrag.rag.AskRequest;
import com.neetrag.rag.AskResponse;
import com.neetrag.rag.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RagController {

    private final RagService ragService;
    private final IngestionService ingestionService;

    public RagController(RagService ragService, IngestionService ingestionService) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
    }

    /** Retrieve sources and generate an answer with Claude. */
    @PostMapping("/ask")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return ragService.ask(request);
    }

    /** Retrieval only, no Claude call. Useful for checking search quality without an API key. */
    @PostMapping("/search")
    public List<AskResponse.Source> search(@Valid @RequestBody AskRequest request) {
        return AskResponse.Source.fromDocuments(ragService.retrieve(request));
    }

    /** Re-read data/questions and data/ncert and rebuild the vector store file. */
    @PostMapping("/ingest")
    public IngestionService.IngestResult ingest() throws IOException {
        return ingestionService.ingestAll();
    }
}
