package com.dump2plan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRenderingTest {

    private static final Path PROMPTS_DIR = Path.of("src/main/resources/prompts");

    @Test
    void allTemplateFilesExist() {
        assertTrue(Files.exists(PROMPTS_DIR.resolve("dump2plan.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("elements/guardrails.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("elements/personalization.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("elements/user.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("personas/analyzer.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("personas/planner.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("personas/reviewer.jinja")));
        assertTrue(Files.exists(PROMPTS_DIR.resolve("objectives/brain-dump-to-plan.jinja")));
    }

    @Test
    void templatesAreNotEmpty() throws IOException {
        var templates = Files.walk(PROMPTS_DIR)
            .filter(p -> p.toString().endsWith(".jinja"))
            .toList();

        assertFalse(templates.isEmpty(), "Should find Jinja templates");

        for (Path template : templates) {
            String content = Files.readString(template);
            assertFalse(content.isBlank(),
                "Template should not be empty: " + template);
        }
    }

    @Test
    void guardrailsContainsSafetyConstraints() throws IOException {
        String content = Files.readString(PROMPTS_DIR.resolve("elements/guardrails.jinja"));
        assertTrue(content.contains("project planning"),
            "Guardrails should reference project planning");
    }

    @Test
    void personalizationIncludesPersonaAndObjective() throws IOException {
        String content = Files.readString(PROMPTS_DIR.resolve("elements/personalization.jinja"));
        assertTrue(content.contains("personas/"),
            "Personalization should include persona templates");
        assertTrue(content.contains("objectives/"),
            "Personalization should include objective templates");
    }
}
