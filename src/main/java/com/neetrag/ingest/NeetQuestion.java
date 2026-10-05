package com.neetrag.ingest;

import java.util.Map;

/** One NEET question as stored in data/questions/*.json. */
public record NeetQuestion(
        String id,
        String subject,
        String chapter,
        String topic,
        Integer year,
        String question,
        Map<String, String> options,
        String answer,
        String explanation) {

    /** Text that gets embedded and shown to Claude as context. */
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(question).append('\n');
        if (options != null) {
            options.forEach((key, value) -> sb.append("(").append(key).append(") ").append(value).append('\n'));
        }
        if (answer != null) {
            sb.append("Correct answer: ").append(answer).append('\n');
        }
        if (explanation != null) {
            sb.append("Explanation: ").append(explanation).append('\n');
        }
        return sb.toString();
    }
}
