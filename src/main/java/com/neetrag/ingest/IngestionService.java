package com.neetrag.ingest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neetrag.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    /** Batch size for embedding calls, so progress is visible on large PDFs. */
    private static final int BATCH_SIZE = 64;

    private final SimpleVectorStore vectorStore;
    private final RagProperties props;
    private final ObjectMapper objectMapper;

    // all-MiniLM-L6-v2 truncates input around 256 word pieces, so keep chunks small
    private final TokenTextSplitter splitter = TokenTextSplitter.builder()
            .withChunkSize(250)
            .withMinChunkSizeChars(100)
            .build();

    public IngestionService(SimpleVectorStore vectorStore, RagProperties props, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public record IngestResult(int questions, int ncertChunks) {
    }

    public IngestResult ingestAll() throws IOException {
        int questions = ingestQuestions();
        int chunks = ingestNcert();
        File file = new File(props.vectorStoreFile());
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        vectorStore.save(file);
        log.info("Saved vector store to {}", file.getAbsolutePath());
        return new IngestResult(questions, chunks);
    }

    /** Loads every *.json file in the questions folder. Each file holds a JSON array of NeetQuestion. */
    private int ingestQuestions() throws IOException {
        List<Document> docs = new ArrayList<>();
        for (Path path : listFiles(Path.of(props.questionsDir()), ".json")) {
            List<NeetQuestion> questions = objectMapper.readValue(
                    Files.readString(path, StandardCharsets.UTF_8), new TypeReference<>() {
                    });
            for (NeetQuestion q : questions) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("type", "question");
                metadata.put("source", path.getFileName().toString());
                putIfPresent(metadata, "questionId", q.id());
                putIfPresent(metadata, "subject", q.subject());
                putIfPresent(metadata, "chapter", q.chapter());
                putIfPresent(metadata, "topic", q.topic());
                putIfPresent(metadata, "year", q.year());
                putIfPresent(metadata, "answer", q.answer());

                // Stable id, so re-ingesting replaces instead of duplicating
                String key = "question:" + path.getFileName() + ":" + (q.id() != null ? q.id() : q.question());
                docs.add(Document.builder()
                        .id(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString())
                        .text(q.toText())
                        .metadata(metadata)
                        .build());
            }
            log.info("Read {} questions from {}", questions.size(), path);
        }
        addInBatches(docs);
        return docs.size();
    }

    /**
     * Loads NCERT PDFs. The subject comes from the folder name:
     * data/ncert/Physics/ch1.pdf gets subject "Physics".
     */
    private int ingestNcert() throws IOException {
        Path root = Path.of(props.ncertDir());
        int total = 0;
        for (Path pdf : listFiles(root, ".pdf")) {
            Path parent = root.relativize(pdf).getParent();
            String subject = parent != null ? parent.getFileName().toString() : "General";
            String fileName = pdf.getFileName().toString();

            List<Document> pages = new PagePdfDocumentReader(new FileSystemResource(pdf)).get();
            List<Document> chunks = new ArrayList<>();
            for (Document chunk : splitter.apply(pages)) {
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("type", "ncert");
                metadata.put("subject", subject);
                metadata.put("source", fileName);
                String key = "ncert:" + subject + "/" + fileName + ":" + chunks.size();
                chunks.add(Document.builder()
                        .id(UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString())
                        .text(chunk.getText())
                        .metadata(metadata)
                        .build());
            }
            log.info("Embedding {} chunks from {}", chunks.size(), pdf);
            addInBatches(chunks);
            total += chunks.size();
        }
        return total;
    }

    private void addInBatches(List<Document> docs) {
        for (int i = 0; i < docs.size(); i += BATCH_SIZE) {
            vectorStore.add(docs.subList(i, Math.min(i + BATCH_SIZE, docs.size())));
        }
    }

    private static List<Path> listFiles(Path dir, String extension) throws IOException {
        if (!Files.isDirectory(dir)) {
            log.warn("Folder not found, skipping: {}", dir.toAbsolutePath());
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(extension))
                    .sorted()
                    .toList();
        }
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
