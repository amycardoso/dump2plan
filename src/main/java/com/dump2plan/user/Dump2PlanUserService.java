package com.dump2plan.user;

import com.embabel.agent.api.identity.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class Dump2PlanUserService implements UserService<Dump2PlanUser>, UserDetailsService {

    private final Map<String, Dump2PlanUser> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public Dump2PlanUserService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        users.put("alice", new Dump2PlanUser("Alice", "alice", "USER"));
        users.put("bob", new Dump2PlanUser("Bob", "bob", "USER"));
    }

    @Override
    public Dump2PlanUser getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        var username = authentication.getName();
        return users.get(username);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(user.username())
                .password(passwordEncoder.encode("password"))
                .roles(user.role())
                .build();
    }
}
