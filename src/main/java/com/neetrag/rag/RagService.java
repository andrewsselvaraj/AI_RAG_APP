package com.neetrag.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.neetrag.config.RagProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
            You are a NEET (India) exam tutor for Physics, Chemistry and Biology, \
            explaining to a Class 11-12 student.

            Each request includes reference material in <sources>: NCERT textbook passages \
            and past NEET questions with answers. Base your answer on these sources and cite \
            them inline as [1], [2], matching the source numbers. If the sources do not cover \
            the question, say so plainly, then give your best explanation and mark that part \
            as not from the provided sources.

            For a multiple-choice question, state the correct option first, then explain why \
            it is correct and why the other options are wrong. Show formulas and calculation \
            steps for numerical problems. Keep explanations clear and exam-focused.""";

    private final VectorStore vectorStore;
    private final AnthropicClient anthropic;
    private final RagProperties props;

    public RagService(VectorStore vectorStore, AnthropicClient anthropic, RagProperties props) {
        this.vectorStore = vectorStore;
        this.anthropic = anthropic;
        this.props = props;
    }

    public AskResponse ask(AskRequest request) {
        List<Document> docs = retrieve(request);
        String answer = generate(request.question(), docs);
        return new AskResponse(answer, AskResponse.Source.fromDocuments(docs));
    }

    public List<Document> retrieve(AskRequest request) {
        SearchRequest.Builder search = SearchRequest.builder()
                .query(request.question())
                .topK(request.topK() != null ? request.topK() : props.topK())
                .similarityThreshold(props.similarityThreshold());

        List<String> filters = new ArrayList<>();
        if (StringUtils.hasText(request.subject())) {
            filters.add("subject == '" + escape(request.subject()) + "'");
        }
        if (StringUtils.hasText(request.type())) {
            filters.add("type == '" + escape(request.type()) + "'");
        }
        if (!filters.isEmpty()) {
            search.filterExpression(String.join(" && ", filters));
        }
        return vectorStore.similaritySearch(search.build());
    }

    private String generate(String question, List<Document> docs) {
        StringBuilder context = new StringBuilder("<sources>\n");
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            context.append("<source id=\"").append(i + 1)
                    .append("\" type=\"").append(d.getMetadata().getOrDefault("type", ""))
                    .append("\" subject=\"").append(d.getMetadata().getOrDefault("subject", ""))
                    .append("\">\n").append(d.getText()).append("\n</source>\n");
        }
        context.append("</sources>");

        RagProperties.Claude claude = props.claude();
        MessageCreateParams params = MessageCreateParams.builder()
                .model(claude.model())
                .maxTokens(claude.maxTokens())
                .outputConfig(BetaOutputConfig.builder()
                        .effort(BetaOutputConfig.Effort.of(claude.effort()))
                        .build())
                // If a safety classifier declines the request, retry it server-side on a fallback model
                .addBeta("server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .system(SYSTEM_PROMPT)
                .addUserMessage(context + "\n\n<question>\n" + question + "\n</question>")
                .build();

        BetaMessage message = anthropic.beta().messages().create(params);

        if (message.stopReason().filter(BetaStopReason.REFUSAL::equals).isPresent()) {
            return "Sorry, this question could not be answered. Please rephrase it.";
        }
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .collect(Collectors.joining("\n"));
    }

    private static String escape(String value) {
        return value.replace("'", "\\'");
    }
}
