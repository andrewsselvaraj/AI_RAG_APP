package com.neetrag.web;

import java.io.IOException;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.UnauthorizedException;
import com.neetrag.ingest.IngestionService;
import com.neetrag.rag.AskRequest;
import com.neetrag.rag.AskResponse;
import com.neetrag.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Thymeleaf screen at http://localhost:8081/ for asking questions in the browser. */
@Controller
public class TutorPageController {

    private static final Logger log = LoggerFactory.getLogger(TutorPageController.class);

    private final RagService ragService;
    private final IngestionService ingestionService;
    private final MarkdownRenderer markdown;

    public TutorPageController(RagService ragService, IngestionService ingestionService, MarkdownRenderer markdown) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
        this.markdown = markdown;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("question", "");
        model.addAttribute("subject", "");
        model.addAttribute("type", "");
        return "index";
    }

    @PostMapping("/")
    public String ask(@RequestParam String question,
                      @RequestParam(defaultValue = "") String subject,
                      @RequestParam(defaultValue = "") String type,
                      @RequestParam(defaultValue = "ask") String action,
                      Model model) {
        model.addAttribute("question", question);
        model.addAttribute("subject", subject);
        model.addAttribute("type", type);

        if (!StringUtils.hasText(question)) {
            model.addAttribute("error", "Please type a question.");
            return "index";
        }

        AskRequest request = new AskRequest(question.trim(), subject, type, null);
        try {
            if ("search".equals(action)) {
                model.addAttribute("sources", AskResponse.Source.fromDocuments(ragService.retrieve(request)));
                model.addAttribute("searchOnly", true);
            } else {
                AskResponse response = ragService.ask(request);
                model.addAttribute("answerHtml", markdown.toHtml(response.answer()));
                model.addAttribute("sources", response.sources());
            }
        } catch (UnauthorizedException e) {
            model.addAttribute("error", "Claude API key missing or invalid. Put it in secrets.yml "
                    + "(see secrets.example.yml) or the ANTHROPIC_API_KEY environment variable, then restart. "
                    + "\"Search only\" works without a key.");
        } catch (AnthropicServiceException e) {
            log.warn("Claude API error", e);
            model.addAttribute("error", "Claude API error " + e.statusCode() + ". Please try again.");
        }
        return "index";
    }

    @PostMapping("/ingest")
    public String ingest(Model model) {
        try {
            IngestionService.IngestResult result = ingestionService.ingestAll();
            model.addAttribute("message", "Index rebuilt: " + result.questions() + " questions and "
                    + result.ncertChunks() + " NCERT chunks.");
        } catch (IOException e) {
            log.error("Ingestion failed", e);
            model.addAttribute("error", "Rebuilding the index failed: " + e.getMessage());
        }
        return home(model);
    }
}
