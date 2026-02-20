package com.dump2plan.agent;

import com.dump2plan.Dump2PlanProperties;
import com.dump2plan.user.Dump2PlanUser;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.ActionContext;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.chat.Conversation;
import com.embabel.chat.UserMessage;

import java.util.Map;

@EmbabelComponent
public class ChatActions {

    private final Dump2PlanProperties properties;

    public ChatActions(Dump2PlanProperties properties) {
        this.properties = properties;
    }

    @Action
    public Dump2PlanUser bindUser(OperationContext context) {
        var forUser = context.getProcessContext().getProcessOptions()
            .getIdentities().getForUser();
        if (forUser instanceof Dump2PlanUser user) {
            return user;
        }
        return null;
    }

    @Action(canRerun = true, trigger = UserMessage.class)
    public void respond(Conversation conversation, Dump2PlanUser user, ActionContext context) {
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
