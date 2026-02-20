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
import com.vaadin.flow.component.icon.VaadinIcon;
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
    private static final String WELCOME_MESSAGE =
        "Welcome to **dump2plan**! Paste your brain dump below and I'll " +
        "transform it into a structured project plan. I may ask a few " +
        "clarifying questions before generating the plan.";

    private final Chatbot chatbot;
    private final Dump2PlanUserService userService;
    private final VerticalLayout messagesLayout;
    private final Scroller messagesScroller;
    private final TextArea inputArea;
    private final Button sendButton;

    record SessionData(
        ChatSession chatSession,
        BlockingQueue<Message> responseQueue,
        BlockingQueue<String> hitlResponseQueue
    ) {}

    public ChatView(Chatbot chatbot, Dump2PlanUserService userService) {
        this.chatbot = chatbot;
        this.userService = userService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var title = new H3("dump2plan");
        title.addClassName("chat-title");

        var resetButton = new Button("New Chat", VaadinIcon.PLUS.create(), e -> resetConversation());
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        resetButton.addClassName("reset-button");

        var header = new HorizontalLayout(title, resetButton);
        header.setWidthFull();
        header.setPadding(true);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);
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
            messagesLayout.add(ChatMessageBubble.assistant(WELCOME_MESSAGE));
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

        new Thread(() -> processUserMessage(text, ui, sessionData)).start();
    }

    private void processUserMessage(String text, UI ui, SessionData sessionData) {
        try {
            sessionData.chatSession().onUserMessage(new UserMessage(text));
            awaitResponses(ui, sessionData);
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
    }

    private void awaitResponses(UI ui, SessionData sessionData) throws InterruptedException {
        while (true) {
            var response = sessionData.responseQueue()
                .poll(RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (response == null) {
                ui.access(() -> {
                    messagesLayout.add(ChatMessageBubble.error(
                        "Response timed out. Please try again."));
                    setInputEnabled(true);
                    scrollToBottom();
                });
                return;
            }

            if (response instanceof AssistantMessage assistantMsg
                    && assistantMsg.getAwaitable() != null) {
                // HITL: render prompt and wait for user input
                ui.access(() -> {
                    var hitlPrompt = HitlPrompt.confirm(
                        assistantMsg.getContent(),
                        hitlResponse -> sessionData.hitlResponseQueue().offer(hitlResponse)
                    );
                    messagesLayout.add(hitlPrompt);
                    scrollToBottom();
                });

                // Block until user submits the HITL response
                var hitlText = sessionData.hitlResponseQueue()
                    .poll(RESPONSE_TIMEOUT_SECONDS * 2, TimeUnit.SECONDS);

                if (hitlText == null) {
                    ui.access(() -> {
                        messagesLayout.add(ChatMessageBubble.error(
                            "HITL response timed out. Please try again."));
                        setInputEnabled(true);
                        scrollToBottom();
                    });
                    return;
                }

                // Resume the agent process with the user's response
                sessionData.chatSession().onUserMessage(new UserMessage(hitlText));
                // Continue loop to await next response (could be another HITL or final)
            } else {
                // Final response — show it and re-enable input
                var content = response.getContent();
                ui.access(() -> {
                    messagesLayout.add(ChatMessageBubble.assistant(content));
                    if (isPlanResponse(content)) {
                        messagesLayout.add(new ExportButtons(content));
                    }
                    setInputEnabled(true);
                    scrollToBottom();
                });
                return;
            }
        }
    }

    private boolean isPlanResponse(String content) {
        return content != null
            && content.length() > 500
            && (content.contains("Milestone") || content.contains("## "));
    }

    private void setInputEnabled(boolean enabled) {
        inputArea.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        if (enabled) {
            inputArea.focus();
        }
    }

    private void resetConversation() {
        VaadinSession.getCurrent().setAttribute(SESSION_DATA_KEY, null);
        messagesLayout.removeAll();
        messagesLayout.add(ChatMessageBubble.assistant(WELCOME_MESSAGE));
        setInputEnabled(true);
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
            var hitlResponseQueue = new ArrayBlockingQueue<String>(1);
            var outputChannel = new VaadinOutputChannel(
                UI.getCurrent(), messagesLayout, responseQueue);
            var currentUser = userService.getDefaultUser();
            var chatSession = chatbot.createSession(currentUser, outputChannel, null, null);
            sessionData = new SessionData(chatSession, responseQueue, hitlResponseQueue);
            VaadinSession.getCurrent().setAttribute(SESSION_DATA_KEY, sessionData);
        }
        return sessionData;
    }
}
