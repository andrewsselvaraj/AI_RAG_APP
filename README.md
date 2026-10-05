# NEET RAG Tutor

A Spring Boot app that answers NEET Physics, Chemistry and Biology questions using
retrieval-augmented generation (RAG):

1. Your NEET questions (JSON) and NCERT textbook PDFs are split into chunks and embedded locally.
2. A question retrieves the most similar chunks from the vector store.
3. Claude (`claude-opus-5-5`) writes an answer grounded in those chunks, citing them as [1], [2].

| Part | Technology |
|---|---|
| Framework | Spring Boot 3.5, Java 17 |
| Embeddings | Spring AI Transformers (all-MiniLM-L6-v2 ONNX, runs locally) |
| Vector store | Spring AI `SimpleVectorStore`, saved to `data/vectorstore.json` |
| PDF reading | Spring AI PDF document reader |
| LLM | Claude via the official Anthropic Java SDK |

## Setup

1. **Embedding model** (one time, ~90 MB, not committed to git):

   ```bash
   mkdir -p models/all-MiniLM-L6-v2
   curl -L -o models/all-MiniLM-L6-v2/tokenizer.json https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json
   curl -L -o models/all-MiniLM-L6-v2/model.onnx https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx
   ```

2. **Claude API key** from https://console.anthropic.com (PowerShell):

   ```powershell
   $env:ANTHROPIC_API_KEY = "sk-ant-..."
   ```

3. **Run** (Maven is downloaded automatically by the wrapper):

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   The app runs on http://localhost:8081. On first start it ingests everything under `data/`
   and saves `data/vectorstore.json`.

## Adding your data

**NEET questions:** put JSON files in `data/questions/`. Each file is an array:

```json
[
  {
    "id": "NEET-2023-PHY-12",
    "subject": "Physics",
    "chapter": "Ray Optics and Optical Instruments",
    "topic": "Lens power",
    "year": 2023,
    "question": "What is the power of a convex lens of focal length 25 cm?",
    "options": { "A": "+2 D", "B": "+4 D", "C": "-4 D", "D": "+0.25 D" },
    "answer": "B",
    "explanation": "P = 1/f = 1/0.25 m = +4 D"
  }
]
```

`data/questions/sample-questions.json` has 6 sample questions to start with.

**NCERT PDFs:** put them in a folder named after the subject, for example
`data/ncert/Physics/keph101.pdf`. The folder name becomes the `subject` used for filtering.

After adding files, rebuild the vector store:

```powershell
Invoke-RestMethod -Method Post http://localhost:8081/api/ingest
```

## API

| Endpoint | Body | Purpose |
|---|---|---|
| `POST /api/ask` | `{"question": "...", "subject": "Physics", "type": "question", "topK": 5}` | Retrieve sources and answer with Claude. Only `question` is required. |
| `POST /api/search` | same as `/ask` | Retrieval only, no Claude call and no API key needed. Use it to check search quality. |
| `POST /api/ingest` | none | Re-read `data/` and rebuild the vector store |

`subject` is `Physics`, `Chemistry` or `Biology`; `type` is `question` or `ncert`.

Example:

```powershell
Invoke-RestMethod -Method Post http://localhost:8081/api/ask -ContentType 'application/json' `
  -Body '{"question": "Where does the oxygen released in photosynthesis come from?"}'
```

## Configuration

Settings are in `src/main/resources/application.yml` under `neet:` (folders, `top-k`,
`similarity-threshold`, Claude model, `max-tokens`, `effort`).

## Moving to PGVector later

Replace the `spring-ai-vector-store` dependency with `spring-ai-starter-vector-store-pgvector`,
remove the `SimpleVectorStore` bean in `AppConfig` (the starter auto-configures a `VectorStore`),
switch `IngestionService` to `VectorStore` and drop its `save` call, and add the
datasource settings. The retrieval code in `RagService` stays the same.
