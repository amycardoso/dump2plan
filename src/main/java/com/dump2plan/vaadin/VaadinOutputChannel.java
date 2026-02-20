package com.dump2plan.vaadin;

import com.embabel.agent.api.channel.MessageOutputChannelEvent;
import com.embabel.agent.api.channel.OutputChannel;
import com.embabel.agent.api.channel.OutputChannelEvent;
import com.embabel.agent.api.channel.ProgressOutputChannelEvent;
import com.embabel.agent.chat.Message;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * OutputChannel implementation that pushes real-time agent progress to the Vaadin UI.
 * Follows the stashbot/urbot VaadinOutputChannel pattern.
 *
 * <p>Handles two event types:</p>
 * <ul>
 *   <li>{@link ProgressOutputChannelEvent} - Shows temporary progress indicators in chat</li>
 *   <li>{@link MessageOutputChannelEvent} - Queues the final response for ChatView to display</li>
 * </ul>
 */
public class VaadinOutputChannel implements OutputChannel {

    private static final Logger log = LoggerFactory.getLogger(VaadinOutputChannel.class);

    private final UI ui;
    private final VerticalLayout messagesLayout;
    private final BlockingQueue<Message> responseQueue;
    private Div currentProgressIndicator;

    public VaadinOutputChannel(UI ui, VerticalLayout messagesLayout,
                               BlockingQueue<Message> responseQueue) {
        this.ui = ui;
        this.messagesLayout = messagesLayout;
        this.responseQueue = responseQueue;
    }

    @Override
    public void receive(OutputChannelEvent event) {
        switch (event) {
            case ProgressOutputChannelEvent progress -> handleProgress(progress);
            case MessageOutputChannelEvent message -> handleMessage(message);
            default -> log.debug("Unhandled output channel event: {}", event.getClass().getSimpleName());
        }
    }

    private void handleProgress(ProgressOutputChannelEvent event) {
        ui.access(() -> {
            removeCurrentProgressIndicator();

            var indicator = new Div();
            indicator.addClassName("tool-call-indicator");

            var spinner = new Span();
            spinner.addClassName("progress-spinner");

            var label = new Span(event.getMessage());
            label.addClassName("progress-label");

            indicator.add(spinner, label);
            messagesLayout.add(indicator);
            currentProgressIndicator = indicator;

            scrollToBottom();
        });
    }

    private void handleMessage(MessageOutputChannelEvent event) {
        ui.access(this::removeCurrentProgressIndicator);

        responseQueue.offer(event.getMessage());
    }

    private void removeCurrentProgressIndicator() {
        if (currentProgressIndicator != null) {
            messagesLayout.remove(currentProgressIndicator);
            currentProgressIndicator = null;
        }
    }

    private void scrollToBottom() {
        messagesLayout.getElement().executeJs(
            "setTimeout(() => { " +
            "  const scroller = this.closest('vaadin-scroller'); " +
            "  if (scroller) scroller.scrollTop = scroller.scrollHeight; " +
            "}, 100);"
        );
    }
}
