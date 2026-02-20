package com.dump2plan.user;

import com.embabel.agent.api.identity.UserService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Dump2PlanUserService implements UserService<Dump2PlanUser> {

    private static final Dump2PlanUser DEFAULT_USER =
        new Dump2PlanUser("User", "user", "USER");

    private final Map<String, Dump2PlanUser> users = new ConcurrentHashMap<>();

    public Dump2PlanUserService() {
        users.put(DEFAULT_USER.getUsername(), DEFAULT_USER);
    }

    @Override
    public Dump2PlanUser findById(String id) {
        return users.get(id);
    }

    @Override
    public Dump2PlanUser findByUsername(String username) {
        return users.get(username);
    }

    @Override
    public Dump2PlanUser findByEmail(String email) {
        return users.values().stream()
            .filter(u -> email.equals(u.getEmail()))
            .findFirst()
            .orElse(null);
    }

    public Dump2PlanUser getDefaultUser() {
        return DEFAULT_USER;
    }
}
