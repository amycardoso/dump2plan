package com.dump2plan.user;

import com.embabel.agent.api.identity.User;

public record Dump2PlanUser(
    String id,
    String displayName,
    String username,
    String email,
    String role
) implements User {

    public Dump2PlanUser(String displayName, String username, String role) {
        this(username, displayName, username, username + "@dump2plan.local", role);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }
}
