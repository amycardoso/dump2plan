package com.dump2plan;

import com.dump2plan.model.Priority;
import com.dump2plan.model.StructuredPlan;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test scaffold for end-to-end plan generation.
 * Requires a live LLM provider (ANTHROPIC_API_KEY or OPENAI_API_KEY).
 *
 * <p>These tests are excluded from {@code mvn test} by surefire config
 * ({@code **\/*IT.java}). Run explicitly with:</p>
 * <pre>
 *   mvn verify -DskipUTs=true -Dit.test=PlanGenerationIT
 * </pre>
 *
 * <p>Future: wire embabel-agent-eval's TranscriptScorer for LLM-as-judge
 * evaluation of plan quality.</p>
 */
@SpringBootTest(
    classes = {TestDump2PlanApplication.class, TestSecurityConfiguration.class},
    properties = "spring.main.web-application-type=none"
)
@ActiveProfiles("it")
@Disabled("Requires live LLM API key - enable for manual integration testing")
class PlanGenerationIT {

    private static final String SIMPLE_BRAIN_DUMP = """
        I want to build a mobile app for tracking daily habits.
        It should have push notifications, a calendar view, and
        streak tracking. Maybe social features later. I have about
        3 months and a team of 2 developers.
        """;

    private static final String COMPLEX_BRAIN_DUMP = """
        We need to migrate our monolith to microservices. Currently we have
        a 500k LOC Java app with PostgreSQL. We want to break out the user
        service, order service, and notification service first. Need to keep
        the monolith running during migration. Team of 8, 6 month timeline,
        must maintain 99.9% uptime during migration. Budget is 200k.
        Also need to set up CI/CD, monitoring, and service mesh.
        """;

    @Test
    void simpleHabitAppBrainDump_generatesValidPlan() {
        // TODO: Wire BrainDumpPlannerAgent via AgentPlatform and invoke with SIMPLE_BRAIN_DUMP
        // For now, validate the test infrastructure compiles and the Spring context loads
        assertNotNull(SIMPLE_BRAIN_DUMP);
    }

    @Test
    void complexMigrationBrainDump_generatesCompletePlan() {
        // TODO: Wire BrainDumpPlannerAgent via AgentPlatform and invoke with COMPLEX_BRAIN_DUMP
        assertNotNull(COMPLEX_BRAIN_DUMP);
    }

    @Test
    void generatedPlan_hasRequiredStructure() {
        // TODO: After generating a plan, validate:
        // - Plan has a title and summary
        // - At least 2 milestones
        // - Tasks have priorities assigned
        // - Dependencies reference valid task IDs
        // - Effort estimates are present
        // - Risks and assumptions are non-empty
    }

    @Test
    void generatedPlan_prioritiesAreBalanced() {
        // TODO: Validate the plan has a reasonable distribution of priorities
        // - Not all CRITICAL
        // - Not all LOW
        // - At least one CRITICAL or HIGH task
        var priorities = Priority.values();
        assertEquals(4, priorities.length, "Should have CRITICAL, HIGH, MEDIUM, LOW");
    }

    // --- Future: LLM-as-judge evaluation ---

    // @Test
    // void planQualityScore_meetsThreshold() {
    //     // Wire TranscriptScorer from embabel-agent-eval
    //     // Score the generated plan against quality rubric:
    //     //   - Completeness: all brain dump topics covered
    //     //   - Feasibility: estimates align with stated constraints
    //     //   - Structure: milestones have logical ordering
    //     //   - Dependencies: task dependency graph is acyclic
    //     // Assert score >= 0.7 (adjust threshold as baseline is established)
    // }
}
