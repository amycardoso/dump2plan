package com.dump2plan;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

/**
 * Integration test for end-to-end plan generation.
 * Requires an active LLM provider (ANTHROPIC_API_KEY or OPENAI_API_KEY).
 * Excluded from unit test runs by surefire plugin (*IT.java pattern).
 */
@SpringBootTest(
    classes = TestDump2PlanApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("it")
@Timeout(value = 15, unit = TimeUnit.MINUTES)
@Disabled("Enable when LLM provider is configured")
class PlanGenerationIT {

    @Test
    void generatePlanFromBrainDump() {
        // TODO: Implement with TranscriptScorer for LLM-as-judge evaluation
        // 1. Create a brain dump input
        // 2. Run through BrainDumpPlannerAgent
        // 3. Verify StructuredPlan output has milestones, tasks, etc.
        // 4. Score quality using TranscriptScorer
    }
}
