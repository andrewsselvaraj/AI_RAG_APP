package com.neetrag.web;

import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/** Converts Claude's markdown answers to HTML. Raw HTML in the input is escaped, so the output is safe to show. */
@Component
public class MarkdownRenderer {

    private final List<Extension> extensions = List.of(TablesExtension.create());
    private final Parser parser = Parser.builder().extensions(extensions).build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true)
            .sanitizeUrls(true)
            .build();

    public String toHtml(String markdown) {
        return markdown == null ? "" : renderer.render(parser.parse(markdown));
    }
}
