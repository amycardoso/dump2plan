package com.dump2plan;

import com.dump2plan.model.*;
import com.dump2plan.service.PlanExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlanExportServiceTest {

    private PlanExportService exportService;
    private StructuredPlan testPlan;

    @BeforeEach
    void setUp() {
        exportService = new PlanExportService();
        testPlan = new StructuredPlan(
            "Test Project",
            "A test project plan",
            List.of(
                new Milestone("m1", "Phase 1", "Setup phase", 0, List.of("t1", "t2")),
                new Milestone("m2", "Phase 2", "Build phase", 1, List.of("t3"))
            ),
            List.of(
                new Task("t1", "Setup repo", "Create repository", Priority.HIGH, "m1", List.of(), "2 hours", 0),
                new Task("t2", "Define stack", "Choose technologies", Priority.MEDIUM, "m1", List.of("t1"), "4 hours", 1),
                new Task("t3", "Build core", "Implement core features", Priority.CRITICAL, "m2", List.of("t2"), "2 weeks", 0)
            ),
            "3 months",
            List.of("Team availability", "Third-party API stability"),
            List.of("Team has Java experience", "Cloud deployment available")
        );
    }

    @Test
    void exportToMarkdown_containsTitle() {
        String markdown = exportService.exportToMarkdown(testPlan);
        assertTrue(markdown.contains("# Test Project"));
    }

    @Test
    void exportToMarkdown_containsMilestones() {
        String markdown = exportService.exportToMarkdown(testPlan);
        assertTrue(markdown.contains("## Phase 1"));
        assertTrue(markdown.contains("## Phase 2"));
    }

    @Test
    void exportToMarkdown_containsTasks() {
        String markdown = exportService.exportToMarkdown(testPlan);
        assertTrue(markdown.contains("Setup repo"));
        assertTrue(markdown.contains("[HIGH]"));
        assertTrue(markdown.contains("[CRITICAL]"));
    }

    @Test
    void exportToMarkdown_containsRisks() {
        String markdown = exportService.exportToMarkdown(testPlan);
        assertTrue(markdown.contains("## Risks"));
        assertTrue(markdown.contains("Team availability"));
    }

    @Test
    void exportToJson_validJson() {
        String json = exportService.exportToJson(testPlan);
        assertTrue(json.contains("\"title\" : \"Test Project\""));
        assertTrue(json.contains("\"milestones\""));
        assertTrue(json.contains("\"tasks\""));
    }

    @Test
    void exportToJson_containsAllTasks() {
        String json = exportService.exportToJson(testPlan);
        assertTrue(json.contains("Setup repo"));
        assertTrue(json.contains("Define stack"));
        assertTrue(json.contains("Build core"));
    }
}
