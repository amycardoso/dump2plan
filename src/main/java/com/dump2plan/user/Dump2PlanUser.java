package com.dump2plan.user;

import com.embabel.agent.api.identity.User;

public record Dump2PlanUser(
    String name,
    String username,
    String role
) implements User {

    @Override
    public String getName() {
        return name;
    }
}
