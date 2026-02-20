package com.dump2plan;

import com.dump2plan.user.Dump2PlanUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Dump2PlanUserServiceTest {

    private Dump2PlanUserService userService;

    @BeforeEach
    void setUp() {
        userService = new Dump2PlanUserService();
    }

    @Test
    void findByUsername_existingUser() {
        var user = userService.findByUsername("user");
        assertNotNull(user);
        assertEquals("User", user.getDisplayName());
        assertEquals("user", user.getUsername());
    }

    @Test
    void findByUsername_nonExistentUser() {
        assertNull(userService.findByUsername("unknown"));
    }

    @Test
    void findById_existingUser() {
        var user = userService.findById("user");
        assertNotNull(user);
        assertEquals("user", user.getId());
    }

    @Test
    void findByEmail_existingUser() {
        var user = userService.findByEmail("user@dump2plan.local");
        assertNotNull(user);
        assertEquals("user", user.getUsername());
    }

    @Test
    void findByEmail_nonExistentEmail() {
        assertNull(userService.findByEmail("nonexistent@example.com"));
    }

    @Test
    void getDefaultUser_returnsUser() {
        var user = userService.getDefaultUser();
        assertNotNull(user);
        assertEquals("User", user.getDisplayName());
    }
}
