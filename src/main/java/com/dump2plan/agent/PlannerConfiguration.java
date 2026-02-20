package com.dump2plan.agent;

import com.dump2plan.Dump2PlanProperties;
import com.embabel.agent.api.common.AgentPlatform;
import com.embabel.agent.chat.AgentProcessChatbot;
import com.embabel.agent.chat.Chatbot;
import com.embabel.agent.chat.InMemoryConversationFactory;
import com.embabel.agent.chat.Verbosity;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan
public class PlannerConfiguration {

    @Bean
    public Chatbot chatbot(AgentPlatform platform, Dump2PlanProperties properties) {
        return AgentProcessChatbot.utilityFromPlatform(
            platform,
            new InMemoryConversationFactory(),
            new Verbosity()
                .withShowPrompts(properties.chat().showPrompts())
                .withShowLlmResponses(properties.chat().showResponses())
        );
    }
}
