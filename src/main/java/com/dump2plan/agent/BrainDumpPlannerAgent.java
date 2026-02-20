package com.dump2plan.agent;

import com.dump2plan.Dump2PlanProperties;
import com.dump2plan.model.ClarifiedContext;
import com.dump2plan.model.ExtractedIdeas;
import com.dump2plan.model.ProjectStructure;
import com.dump2plan.model.StructuredPlan;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.agent.domain.io.UserInput;

@Agent(description = "Transforms unstructured brain dumps into structured project plans")
public class BrainDumpPlannerAgent {

    private final Dump2PlanProperties properties;

    public BrainDumpPlannerAgent(Dump2PlanProperties properties) {
        this.properties = properties;
    }

    @Action(cost = 0.1)
    public ExtractedIdeas analyzeInput(UserInput input, Ai ai) {
        return ai
            .withLlm(properties.actors().analyzer().llm())
            .createObject(
                "Analyze this brain dump. Extract topics, action items, constraints, " +
                "project type, complexity, and generate clarifying questions to ask the user " +
                "before creating a plan:\n\n" + input.getContent(),
                ExtractedIdeas.class
            );
    }

    @Action(cost = 0.05)
    public ClarifiedContext gatherContext(ExtractedIdeas ideas) {
        return WaitFor.formSubmission(
            "Before I create your plan, I have a few questions:\n" +
            String.join("\n", ideas.clarifyingQuestions()),
            ClarifiedContext.class
        );
    }

    @Action(cost = 0.3)
    public ProjectStructure structurePlan(
            ExtractedIdeas ideas,
            ClarifiedContext context,
            Ai ai) {
        return ai
            .withLlm(properties.actors().planner().llm())
            .createObject(
                "Create a structured project plan with milestones and tasks based on " +
                "the following analysis and user context.\n\n" +
                "Extracted ideas: " + ideas + "\n\n" +
                "User context - Timeline: " + context.timeline() +
                ", Team size: " + context.teamSize() +
                ", Budget: " + context.budgetConstraints() +
                ", Additional: " + context.additionalContext(),
                ProjectStructure.class
            );
    }

    @AchievesGoal(description = "A validated, prioritized, structured project plan")
    @Action(cost = 0.2)
    public StructuredPlan finalizePlan(
            ProjectStructure structure,
            ExtractedIdeas ideas,
            Ai ai) {
        return ai
            .withLlm(properties.actors().reviewer().llm())
            .createObject(
                "Finalize this project plan: validate completeness, prioritize tasks, " +
                "assign dependencies, estimate effort, and identify risks and assumptions.\n\n" +
                "Structure: " + structure + "\n\n" +
                "Original ideas: " + ideas,
                StructuredPlan.class
            );
    }
}
