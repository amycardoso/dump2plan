package com.dump2plan.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("Organized project structure with milestones and grouped tasks")
public record ProjectStructure(
    @JsonPropertyDescription("Project title")
    String title,

    @JsonPropertyDescription("High-level project summary")
    String summary,

    @JsonPropertyDescription("Ordered list of milestones")
    List<Milestone> milestones,

    @JsonPropertyDescription("All tasks in the project")
    List<Task> tasks,

    @JsonPropertyDescription("Overall estimated duration")
    String estimatedDuration
) {}
