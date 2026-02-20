package com.dump2plan.vaadin;

import com.dump2plan.service.PlanExportService;
import com.embabel.agent.chat.Chatbot;
import com.embabel.agent.chat.ChatSession;
import com.embabel.agent.core.UserMessage;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;

@Route("")
@PermitAll
public class ChatView extends VerticalLayout {

    private final Chatbot chatbot;
    private final PlanExportService exportService;
    private final VerticalLayout messagesLayout;
    private final TextArea inputArea;

    public ChatView(Chatbot chatbot, PlanExportService exportService) {
        this.chatbot = chatbot;
        this.exportService = exportService;

        setSizeFull();
        setPadding(true);
        setSpacing(false);

        var header = new H2("dump2plan");
        header.addClassName("chat-header");

        messagesLayout = new VerticalLayout();
        messagesLayout.setWidthFull();
        messagesLayout.setPadding(false);
        messagesLayout.setSpacing(true);
        messagesLayout.addClassName("chat-messages");

        var scroller = new Scroller(messagesLayout);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.addClassName("chat-scroller");

        inputArea = new TextArea();
        inputArea.setWidthFull();
        inputArea.setPlaceholder("Paste your brain dump here...");
        inputArea.setMinHeight("80px");
        inputArea.setMaxHeight("200px");
        inputArea.addClassName("chat-input");

        var sendButton = new Button("Send", e -> sendMessage());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickShortcut(Key.ENTER);

        var inputLayout = new HorizontalLayout(inputArea, sendButton);
        inputLayout.setWidthFull();
        inputLayout.setAlignItems(Alignment.END);
        inputLayout.expand(inputArea);

        addMessageBubble("agent",
            "Welcome to **dump2plan**! Paste your brain dump below and I'll " +
            "transform it into a structured project plan. I may ask a few " +
            "clarifying questions before generating the plan.");

        add(header, scroller, inputLayout);
        expand(scroller);
    }

    private void sendMessage() {
        var text = inputArea.getValue();
        if (text == null || text.isBlank()) {
            return;
        }

        inputArea.clear();
        inputArea.setEnabled(false);
        addMessageBubble("user", text);

        var ui = UI.getCurrent();
        new Thread(() -> {
            try {
                var session = getOrCreateSession();
                session.onUserMessage(new UserMessage(text));

                ui.access(() -> {
                    addMessageBubble("agent", "Processing your brain dump...");
                    inputArea.setEnabled(true);
                    inputArea.focus();
                });
            } catch (Exception e) {
                ui.access(() -> {
                    addMessageBubble("agent",
                        "Sorry, an error occurred: " + e.getMessage());
                    inputArea.setEnabled(true);
                });
            }
        }).start();
    }

    private void addMessageBubble(String role, String content) {
        messagesLayout.add(new ChatMessageBubble(role, content));
    }

    private ChatSession getOrCreateSession() {
        var session = VaadinSession.getCurrent()
            .getAttribute(ChatSession.class);
        if (session == null) {
            session = chatbot.createSession();
            VaadinSession.getCurrent().setAttribute(ChatSession.class, session);
        }
        return session;
    }
}
