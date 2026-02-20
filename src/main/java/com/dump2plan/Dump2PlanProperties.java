package com.dump2plan;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "dump2plan")
public record Dump2PlanProperties(
    @NestedConfigurationProperty ChatConfig chat,
    @NestedConfigurationProperty ActorsConfig actors,
    String persona,
    String objective
) {
    public record ChatConfig(
        String llm,
        boolean showPrompts,
        boolean showResponses
    ) {}

    public record ActorsConfig(
        ActorConfig analyzer,
        ActorConfig planner,
        ActorConfig reviewer
    ) {}

    public record ActorConfig(
        String persona,
        String llm
    ) {}
}
