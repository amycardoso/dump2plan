package com.dump2plan.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("Final structured project plan with priorities, dependencies, and validation")
public record StructuredPlan(
    @JsonPropertyDescription("Plan title derived from the brain dump")
    String title,

    @JsonPropertyDescription("High-level project summary")
    String summary,

    @JsonPropertyDescription("Ordered list of milestones")
    List<Milestone> milestones,

    @JsonPropertyDescription("All tasks with priorities and dependencies")
    List<Task> tasks,

    @JsonPropertyDescription("Overall estimated duration")
    String estimatedDuration,

    @JsonPropertyDescription("Identified risks")
    List<String> risks,

    @JsonPropertyDescription("Key assumptions made during planning")
    List<String> assumptions
) {}
