package com.dump2plan.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("Ideas and action items extracted from a brain dump")
public record ExtractedIdeas(
    @JsonPropertyDescription("Key topics identified in the brain dump")
    List<String> extractedTopics,

    @JsonPropertyDescription("Actionable items extracted from the brain dump")
    List<String> extractedActions,

    @JsonPropertyDescription("Constraints or limitations mentioned")
    List<String> extractedConstraints,

    @JsonPropertyDescription("Type of project identified")
    String projectType,

    @JsonPropertyDescription("Estimated complexity of the project")
    String estimatedComplexity,

    @JsonPropertyDescription("Questions to ask the user for clarification")
    List<String> clarifyingQuestions
) {}
