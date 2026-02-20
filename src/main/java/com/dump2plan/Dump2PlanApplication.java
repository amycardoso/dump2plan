package com.dump2plan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Dump2PlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(Dump2PlanApplication.class, args);
    }
}
