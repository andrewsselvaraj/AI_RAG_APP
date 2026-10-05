package com.neetrag.rag;

import jakarta.validation.constraints.NotBlank;

/**
 * @param subject optional filter: Physics, Chemistry or Biology
 * @param type    optional filter: "question" or "ncert"
 * @param topK    optional override for how many sources to retrieve
 */
public record AskRequest(@NotBlank String question, String subject, String type, Integer topK) {
}
