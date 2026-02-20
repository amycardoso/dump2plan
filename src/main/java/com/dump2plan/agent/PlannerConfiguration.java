package com.dump2plan.agent;

import com.dump2plan.Dump2PlanProperties;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.Verbosity;
import com.embabel.chat.Chatbot;
import com.embabel.chat.agent.AgentProcessChatbot;
import com.embabel.chat.support.InMemoryConversationFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
