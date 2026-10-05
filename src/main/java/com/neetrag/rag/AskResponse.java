package com.neetrag.rag;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.ai.document.Document;

public record AskResponse(String answer, List<Source> sources) {

    /** A retrieved chunk. {@code number} matches the [n] citations in the answer. */
    public record Source(int number, Map<String, Object> metadata, Double score, String text) {

        public static List<Source> fromDocuments(List<Document> docs) {
            return IntStream.range(0, docs.size())
                    .mapToObj(i -> new Source(i + 1, docs.get(i).getMetadata(),
                            docs.get(i).getScore(), docs.get(i).getText()))
                    .toList();
        }
    }
}
