package com.dump2plan.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("User responses to clarifying questions, gathered via HITL")
public record ClarifiedContext(
    @JsonPropertyDescription("Target timeline for the project")
    String timeline,

    @JsonPropertyDescription("Size of the team working on the project")
    String teamSize,

    @JsonPropertyDescription("Budget constraints or limitations")
    String budgetConstraints,

    @JsonPropertyDescription("Any additional context provided by the user")
    String additionalContext
) {}
