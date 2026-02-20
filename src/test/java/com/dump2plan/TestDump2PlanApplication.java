package com.dump2plan;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = "com.dump2plan",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.dump2plan\\.vaadin\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.dump2plan\\.security\\..*")
    }
)
public class TestDump2PlanApplication {
}
