package com.dump2plan.agent;

import com.dump2plan.Dump2PlanProperties;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.api.identity.User;
import com.embabel.agent.core.Conversation;
import com.embabel.agent.core.UserMessage;

import java.util.Map;

@EmbabelComponent
public class ChatActions {

    private final Dump2PlanProperties properties;

    public ChatActions(Dump2PlanProperties properties) {
        this.properties = properties;
    }

    @Action
    public User bindUser(OperationContext context) {
        return context.getProcessContext().getProcessOptions()
            .getIdentities().getForUser();
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    public void respond(Conversation conversation, User user, ActionContext context) {
        var assistantMessage = context.ai()
            .withLlm(properties.chat().llm())
            .rendering("dump2plan")
            .respondWithSystemPrompt(conversation, Map.of(
                "properties", properties,
                "user", user
            ));
        context.sendMessage(conversation.addMessage(assistantMessage));
    }
}
