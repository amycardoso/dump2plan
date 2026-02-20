package com.dump2plan.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

@JsonClassDescription("A milestone representing a significant checkpoint")
public record Milestone(
    @JsonPropertyDescription("Unique milestone identifier")
    String id,

    @JsonPropertyDescription("Milestone name")
    String name,

    @JsonPropertyDescription("Milestone description")
    String description,

    @JsonPropertyDescription("Order in the project timeline")
    int orderIndex,

    @JsonPropertyDescription("IDs of tasks belonging to this milestone")
    List<String> taskIds
) {}
