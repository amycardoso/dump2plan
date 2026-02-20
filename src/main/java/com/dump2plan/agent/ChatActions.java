package com.dump2plan.agent;

import com.dump2plan.user.Dump2PlanUser;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.EmbabelComponent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.chat.UserMessage;

@EmbabelComponent
public class ChatActions {

    @Action
    public Dump2PlanUser bindUser(OperationContext context) {
        var forUser = context.getProcessContext().getProcessOptions()
            .getIdentities().getForUser();
        if (forUser instanceof Dump2PlanUser user) {
            return user;
        }
        return null;
    }

    @Action(trigger = UserMessage.class)
    public UserInput extractInput(UserMessage message) {
        return new UserInput(message.getContent());
    }
}
