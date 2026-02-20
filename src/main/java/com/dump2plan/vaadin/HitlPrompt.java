package com.dump2plan.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.util.function.Consumer;

/**
 * Renders HITL (Human-in-the-Loop) prompts inline in the chat.
 * Supports confirm() prompts where the agent asks clarifying questions
 * and the user responds with freeform text.
 */
public class HitlPrompt extends VerticalLayout {

    private HitlPrompt() {
        addClassName("hitl-prompt");
        setPadding(true);
        setSpacing(true);
        setWidthFull();
    }

    /**
     * Creates a confirm-style HITL prompt. The agent displays a message
     * (typically clarifying questions) and waits for the user's freeform response.
     *
     * @param message the prompt message from the agent (rendered as markdown)
     * @param onSubmit callback invoked with the user's response text
     * @return the HitlPrompt component
     */
    public static HitlPrompt confirm(String message, Consumer<String> onSubmit) {
        var prompt = new HitlPrompt();

        var header = new Span("Agent needs your input");
        header.addClassName("hitl-prompt-header");

        var messageDiv = new Div();
        messageDiv.addClassName("hitl-prompt-message");
        messageDiv.getElement().setProperty("innerHTML",
            ChatMessageBubble.renderMarkdown(message));

        var responseArea = new TextArea();
        responseArea.setWidthFull();
        responseArea.setPlaceholder("Type your response...");
        responseArea.setMinHeight("60px");
        responseArea.setMaxHeight("150px");
        responseArea.addClassName("hitl-response-input");

        var sendButton = new Button("Submit");
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> {
            var response = responseArea.getValue();
            if (response != null && !response.isBlank()) {
                responseArea.setEnabled(false);
                sendButton.setEnabled(false);
                prompt.addClassName("hitl-prompt-submitted");
                onSubmit.accept(response);
            }
        });

        var buttonLayout = new HorizontalLayout(sendButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);

        prompt.add(header, messageDiv, responseArea, buttonLayout);
        return prompt;
    }
}
