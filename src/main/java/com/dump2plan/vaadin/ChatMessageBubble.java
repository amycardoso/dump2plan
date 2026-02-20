package com.dump2plan.vaadin;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Renders a chat message bubble with markdown support for assistant messages.
 * Follows the stashbot/urbot ChatMessageBubble pattern.
 */
public class ChatMessageBubble extends Div {

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder()
        .escapeHtml(true)
        .build();

    private ChatMessageBubble(String senderName, String text, boolean isUser) {
        addClassName("chat-bubble-container");
        addClassName(isUser ? "user" : "assistant");

        var bubble = new Div();
        bubble.addClassName("chat-bubble");
        bubble.addClassName(isUser ? "user" : "assistant");

        var sender = new Span(senderName);
        sender.addClassName("chat-bubble-sender");

        var content = new Div();
        content.addClassName("chat-bubble-text");

        if (isUser) {
            content.setText(text);
        } else {
            String html = renderMarkdown(text);
            if (!html.isEmpty()) {
                content.getElement().setProperty("innerHTML", html);
            }
        }

        bubble.add(sender, content);
        add(bubble);
    }

    public static ChatMessageBubble user(String text) {
        return new ChatMessageBubble("You", text, true);
    }

    public static ChatMessageBubble assistant(String text) {
        return new ChatMessageBubble("dump2plan", text, false);
    }

    public static Div error(String text) {
        var div = new Div();
        div.addClassName("chat-bubble-error");
        div.setText(text);
        return div;
    }

    static String renderMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        var document = MARKDOWN_PARSER.parse(markdown.strip());
        return HTML_RENDERER.render(document).strip();
    }
}
