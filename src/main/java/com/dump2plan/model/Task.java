package com.dump2plan.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("A concrete task within the project plan")
public record Task(
    @JsonPropertyDescription("Unique task identifier")
    String id,

    @JsonPropertyDescription("Task title")
    String title,

    @JsonPropertyDescription("Detailed task description")
    String description,

    @JsonPropertyDescription("Task priority level")
    Priority priority,

    @JsonPropertyDescription("ID of the milestone this task belongs to")
    String milestoneId,

    @JsonPropertyDescription("IDs of tasks this task depends on")
    List<String> dependsOn,

    @JsonPropertyDescription("Estimated effort for this task")
    String estimatedEffort,

    @JsonPropertyDescription("Order within the milestone")
    int orderIndex
) {}
