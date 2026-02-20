package com.dump2plan.vaadin;

import com.dump2plan.user.Dump2PlanUserService;
import com.embabel.chat.AssistantMessage;
import com.embabel.chat.Chatbot;
import com.embabel.chat.ChatSession;
import com.embabel.chat.Message;
import com.embabel.chat.UserMessage;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Route("")
@PageTitle("dump2plan")
public class ChatView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(ChatView.class);
    private static final int RESPONSE_TIMEOUT_SECONDS = 120;
    private static final String SESSION_DATA_KEY = "dump2plan.sessionData";

    private final Chatbot chatbot;
    private final Dump2PlanUserService userService;
    private final VerticalLayout messagesLayout;
    private final Scroller messagesScroller;
    private final TextArea inputArea;
    private final Button sendButton;

    record SessionData(ChatSession chatSession, BlockingQueue<Message> responseQueue) {}

    public ChatView(Chatbot chatbot, Dump2PlanUserService userService) {
        this.chatbot = chatbot;
        this.userService = userService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var title = new H3("dump2plan");
        title.addClassName("chat-title");
        var header = new HorizontalLayout(title);
        header.setWidthFull();
        header.setPadding(true);
        header.addClassName("chat-header");

        messagesLayout = new VerticalLayout();
        messagesLayout.setWidthFull();
        messagesLayout.setPadding(true);
        messagesLayout.setSpacing(true);
        messagesLayout.addClassName("chat-messages");

        messagesScroller = new Scroller(messagesLayout);
        messagesScroller.setSizeFull();
        messagesScroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        messagesScroller.addClassName("chat-scroller");

        inputArea = new TextArea();
        inputArea.setWidthFull();
        inputArea.setPlaceholder("Paste your brain dump here...");
        inputArea.setMinHeight("80px");
        inputArea.setMaxHeight("200px");
        inputArea.addClassName("chat-input");

        sendButton = new Button("Send", e -> sendMessage());
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickShortcut(Key.ENTER);

        var inputLayout = new HorizontalLayout(inputArea, sendButton);
        inputLayout.setWidthFull();
        inputLayout.setPadding(true);
        inputLayout.setAlignItems(Alignment.END);
        inputLayout.expand(inputArea);
        inputLayout.addClassName("chat-input-layout");

        add(header, messagesScroller, inputLayout);
        expand(messagesScroller);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        restorePreviousMessages();
        if (messagesLayout.getComponentCount() == 0) {
            messagesLayout.add(ChatMessageBubble.assistant(
                "Welcome to **dump2plan**! Paste your brain dump below and I'll " +
                "transform it into a structured project plan. I may ask a few " +
                "clarifying questions before generating the plan."
            ));
        }
    }

    private void sendMessage() {
        var text = inputArea.getValue();
        if (text == null || text.isBlank()) {
            return;
        }

        inputArea.clear();
        setInputEnabled(false);
        messagesLayout.add(ChatMessageBubble.user(text));
        scrollToBottom();

        var ui = UI.getCurrent();
        var sessionData = getOrCreateSessionData();

        new Thread(() -> {
            try {
                sessionData.chatSession().onUserMessage(new UserMessage(text));

                var response = sessionData.responseQueue()
                    .poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                ui.access(() -> {
                    if (response != null) {
                        messagesLayout.add(ChatMessageBubble.assistant(response.getContent()));
                    } else {
                        messagesLayout.add(ChatMessageBubble.error(
                            "Response timed out. Please try again."));
                    }
                    setInputEnabled(true);
                    scrollToBottom();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ui.access(() -> {
                    messagesLayout.add(ChatMessageBubble.error(
                        "Request was interrupted. Please try again."));
                    setInputEnabled(true);
                });
            } catch (Exception e) {
                log.error("Error processing message", e);
                ui.access(() -> {
                    messagesLayout.add(ChatMessageBubble.error(
                        "An error occurred: " + e.getMessage()));
                    setInputEnabled(true);
                });
            }
        }).start();
    }

    private void setInputEnabled(boolean enabled) {
        inputArea.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        if (enabled) {
            inputArea.focus();
        }
    }

    private void scrollToBottom() {
        messagesScroller.getElement().executeJs(
            "setTimeout(() => { this.scrollTop = this.scrollHeight; }, 50);"
        );
    }

    private void restorePreviousMessages() {
        var sessionData = (SessionData) VaadinSession.getCurrent()
            .getAttribute(SESSION_DATA_KEY);
        if (sessionData == null) {
            return;
        }

        var conversation = sessionData.chatSession().getConversation();
        if (conversation == null) {
            return;
        }

        for (var message : conversation.getMessages()) {
            if (message instanceof UserMessage userMsg) {
                messagesLayout.add(ChatMessageBubble.user(userMsg.getContent()));
            } else if (message instanceof AssistantMessage assistantMsg) {
                messagesLayout.add(ChatMessageBubble.assistant(assistantMsg.getContent()));
            }
        }
    }

    private SessionData getOrCreateSessionData() {
        var sessionData = (SessionData) VaadinSession.getCurrent()
            .getAttribute(SESSION_DATA_KEY);
        if (sessionData == null) {
            var responseQueue = new ArrayBlockingQueue<Message>(10);
            var outputChannel = new VaadinOutputChannel(
                UI.getCurrent(), messagesLayout, responseQueue);
            var currentUser = userService.getDefaultUser();
            var chatSession = chatbot.createSession(currentUser, outputChannel, null, null);
            sessionData = new SessionData(chatSession, responseQueue);
            VaadinSession.getCurrent().setAttribute(SESSION_DATA_KEY, sessionData);
        }
        return sessionData;
    }
}
