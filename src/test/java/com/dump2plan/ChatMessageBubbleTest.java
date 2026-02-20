package com.dump2plan;

import com.dump2plan.vaadin.ChatMessageBubble;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatMessageBubbleTest {

    @Test
    void renderMarkdown_boldText() {
        String html = ChatMessageBubble.renderMarkdown("**bold**");
        assertTrue(html.contains("<strong>bold</strong>"));
    }

    @Test
    void renderMarkdown_italicText() {
        String html = ChatMessageBubble.renderMarkdown("*italic*");
        assertTrue(html.contains("<em>italic</em>"));
    }

    @Test
    void renderMarkdown_bulletList() {
        String html = ChatMessageBubble.renderMarkdown("- item 1\n- item 2");
        assertTrue(html.contains("<li>"));
        assertTrue(html.contains("item 1"));
        assertTrue(html.contains("item 2"));
    }

    @Test
    void renderMarkdown_codeBlock() {
        String html = ChatMessageBubble.renderMarkdown("`inline code`");
        assertTrue(html.contains("<code>inline code</code>"));
    }

    @Test
    void renderMarkdown_escapesRawHtml() {
        String html = ChatMessageBubble.renderMarkdown("<script>alert('xss')</script>");
        assertFalse(html.contains("<script>"), "Raw HTML should be escaped");
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void renderMarkdown_escapesHtmlInLinks() {
        String html = ChatMessageBubble.renderMarkdown(
            "[click](javascript:alert('xss'))");
        assertFalse(html.contains("javascript:"));
    }

    @Test
    void renderMarkdown_nullReturnsEmpty() {
        assertEquals("", ChatMessageBubble.renderMarkdown(null));
    }

    @Test
    void renderMarkdown_blankReturnsEmpty() {
        assertEquals("", ChatMessageBubble.renderMarkdown("   "));
    }

    @Test
    void renderMarkdown_headings() {
        String html = ChatMessageBubble.renderMarkdown("## Phase 1");
        assertTrue(html.contains("<h2>Phase 1</h2>"));
    }
}
