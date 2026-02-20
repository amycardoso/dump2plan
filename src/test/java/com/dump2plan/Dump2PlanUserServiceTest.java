package com.dump2plan;

import com.dump2plan.user.Dump2PlanUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class Dump2PlanUserServiceTest {

    private Dump2PlanUserService userService;

    @BeforeEach
    void setUp() {
        userService = new Dump2PlanUserService(new BCryptPasswordEncoder());
    }

    @Test
    void findByUsername_existingUser() {
        var user = userService.findByUsername("alice");
        assertNotNull(user);
        assertEquals("Alice", user.getDisplayName());
        assertEquals("alice", user.getUsername());
    }

    @Test
    void findByUsername_nonExistentUser() {
        assertNull(userService.findByUsername("unknown"));
    }

    @Test
    void findById_existingUser() {
        var user = userService.findById("alice");
        assertNotNull(user);
        assertEquals("alice", user.getId());
    }

    @Test
    void findByEmail_existingUser() {
        var user = userService.findByEmail("alice@dump2plan.local");
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
    }

    @Test
    void findByEmail_nonExistentEmail() {
        assertNull(userService.findByEmail("nonexistent@example.com"));
    }

    @Test
    void loadUserByUsername_existingUser() {
        var userDetails = userService.loadUserByUsername("alice");
        assertEquals("alice", userDetails.getUsername());
        assertTrue(userDetails.getPassword().startsWith("$2a$"));
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_nonExistentUser_throws() {
        assertThrows(UsernameNotFoundException.class,
            () -> userService.loadUserByUsername("unknown"));
    }
}
