package com.dump2plan;

import com.dump2plan.user.Dump2PlanUser;
import com.dump2plan.user.Dump2PlanUserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Dump2PlanUserService dump2PlanUserService(PasswordEncoder passwordEncoder) {
        return new Dump2PlanUserService(passwordEncoder);
    }
}
